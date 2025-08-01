package tanks.network;

import basewindow.BaseFile;
import com.codedisaster.steamworks.*;
import tanks.*;
import tanks.gui.screen.*;

import java.util.*;

public class SteamWorkshopHandler
{
    public SteamNetworkHandler handler;

    public SteamUGC workshop;

    public String uploadingType = null;
    public String uploadingContents = null;
    public String uploadingName = null;
    public String uploadingDescription = null;
    public String uploadMainDir = Game.directoryPath + "/upload";
    public String screenshotDir = Game.directoryPath + "/screenshot.png";
    public String uploadDir = uploadMainDir;

    public static final int results_count = 50;

    public String searchType = null;
    public SteamID searchUser = null;
    public String searchText = null;
    public boolean searchByScore = false;

    public HashMap<Integer, SteamUGCDetails> publishedFiles = new HashMap<>();

    public SteamUGCQuery currentQuery = null;
    public HashSet<Integer> pagesQueried = new HashSet<>();
    public Queue<Integer> pendingQueries = new LinkedList<>();
    public int totalResults = -1;

    public SteamUGCDetails downloadFile = null;
    public String downloadingName = "level";
    public BaseFile downloadFolder = null;

    public int currentDownloadVote = -2;

    public SteamWorkshopHandler(SteamNetworkHandler handler)
    {
        this.handler = handler;
        this.workshop = new SteamUGC(workshopCallback);
    }

    public SteamUGCCallback workshopCallback = new SteamUGCCallback()
    {
        @Override
        public void onCreateItem(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result)
        {
            if (result != SteamResult.OK)
                Game.screen = new ScreenWorkshopActionResult(new ScreenSteamWorkshop(), "Upload failed!", SteamResults.getMessage(result), false);
            else
            {
                SteamUGCUpdateHandle v = workshop.startItemUpdate(handler.clientUtils.getAppID(), publishedFileID);

                BaseFile f = Game.game.fileManager.getFile(Game.homedir + uploadDir + "/" + uploadingName);
                try
                {
                    f.create();
                    f.startWriting();
                    f.println(uploadingContents);
                    f.stopWriting();
                    workshop.setItemTitle(v, uploadingName.replace("_", " "));
                    workshop.setItemDescription(v, uploadingDescription);
                    workshop.setItemTags(v, new String[]{uploadingType, Game.version});
                    workshop.setItemPreview(v, Game.homedir + screenshotDir);
                    workshop.setItemVisibility(v, SteamRemoteStorage.PublishedFileVisibility.Public);
                    workshop.setItemContent(v, Game.homedir + uploadDir);
                    workshop.submitItemUpdate(v, "");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Game.exitToCrash(e);
                }
            }
        }

        @Override
        public void onSubmitItemUpdate(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result)
        {
            if (result == SteamResult.OK)
            {
                if (!needsToAcceptWLA)
                    Game.screen = new ScreenWorkshopActionResult(new ScreenSteamWorkshop(), "Uploaded!", uploadingType + " was uploaded to Steam Workshop! \n Please note that it may take a few minutes for it to show up in listings.", true);
                else
                    Game.screen = new ScreenWorkshopMustAcceptAgreement(new ScreenSteamWorkshop(), "Uploaded!", uploadingType + " was uploaded to Steam Workshop! \n However, you haven't yet accepted the Steam Workshop Legal agreement. \n Your " + uploadingType.toLowerCase() + " will be private until you do so.", true);

                Game.game.fileManager.getFile(Game.homedir + uploadDir + "/" + uploadingName).delete();
                Game.game.fileManager.getFile(Game.homedir + screenshotDir).delete();
                Game.game.fileManager.getFile(Game.homedir + uploadMainDir).delete();
                uploadingName = null;
                uploadingType = null;
                uploadingContents = null;
            }
        }

        @Override
        public void onUGCQueryCompleted(SteamUGCQuery query, int numResultsReturned, int totalMatchingResults, boolean isCachedData, SteamResult result)
        {
            if (result != SteamResult.OK)
                Game.screen = new ScreenWorkshopActionResult(new ScreenSteamWorkshop(), "Query failed!", SteamResults.getMessage(result), false);
            else if (currentQuery.equals(query))
            {
                currentQuery = null;
                totalResults = totalMatchingResults;

                int page = pendingQueries.remove();
                for (int i = 0; i < numResultsReturned; i++)
                {
                    SteamUGCDetails d = new SteamUGCDetails();
                    workshop.getQueryUGCResult(query, i, d);
                    if (!handler.friends.knownUsernamesByID.containsKey(d.getOwnerID().getAccountID()))
                    {
                        if (!handler.friends.friends.requestUserInformation(d.getOwnerID(), true))
                            handler.friends.knownUsernamesByID.put(d.getOwnerID().getAccountID(), handler.friends.friends.getFriendPersonaName(d.getOwnerID()));
                    }
                    publishedFiles.put(i + page * results_count, d);
                }

                workshop.releaseQueryUserUGCRequest(query);
                updateQueries();
            }
        }

        @Override
        public void onDownloadItemResult(int appID, SteamPublishedFileID publishedFileID, SteamResult result)
        {
            if (appID == handler.clientUtils.getAppID() && downloadFile.getPublishedFileID().equals(publishedFileID))
            {
                if (result == SteamResult.OK)
                {
                    try
                    {
                        SteamUGC.ItemInstallInfo i = new SteamUGC.ItemInstallInfo();
                        workshop.getItemInstallInfo(publishedFileID, i);
                        downloadFolder = Game.game.fileManager.getFile(i.getFolder());
                    }
                    catch (Exception e)
                    {
                        Game.exitToCrash(e);
                    }
                }
                else
                    Game.screen = new ScreenWorkshopLevelDownloadFailed(result, downloadFile, new ScreenSteamWorkshop());
            }
        }

        @Override
        public void onDeleteItem(SteamPublishedFileID publishedFileID, SteamResult result)
        {
            if (result.equals(SteamResult.OK))
                Game.screen = new ScreenWorkshopActionResult(new ScreenSteamWorkshop(), "Success!", uploadingType + " was removed from Steam Workshop! \n Please note that it may take a few minutes for it to disappear from listings.", true);
            else
                Game.screen = new ScreenWorkshopActionResult(new ScreenSteamWorkshop(), "Failed to remove " + uploadingType.toLowerCase() + "!", SteamResults.getMessage(result), false);
        }

        @Override
        public void onGetUserItemVote(SteamPublishedFileID publishedFileID, boolean votedUp, boolean votedDown, boolean voteSkipped, SteamResult result)
        {
            if (result == SteamResult.OK && publishedFileID.equals(downloadFile.getPublishedFileID()))
            {
                currentDownloadVote = 0;

                if (votedUp)
                    currentDownloadVote = 1;
                else if (votedDown)
                    currentDownloadVote = -1;
            }
        }

        public void onSetUserItemVote(SteamPublishedFileID publishedFileID, boolean voteUp, SteamResult result)
        {
            if (!result.equals(SteamResult.OK))
            {
                Drawing.drawing.playSound("leave.ogg");
                ScreenOverlayChat.addChat("\u00A7255000000255Failed to submit vote. " + SteamResults.getMessage(result));
            }
        }
    };

    public void upload(String type, String name, String contents, String description)
    {
        if (uploadingName != null)
            return;

        Game.game.fileManager.getFile(Game.homedir + uploadDir).mkdirs();
        Panel.panel.saveScreenshotDir = Game.homedir + screenshotDir;

        uploadingName = name;
        uploadingType = type;
        uploadingContents = contents;
        uploadingDescription = description;
        workshop.createItem(handler.clientUtils.getAppID(), SteamRemoteStorage.WorkshopFileType.Community);
    }

    public void search(String type, int start, int end, SteamID user, String text, boolean byScore)
    {
        if (!Objects.equals(type, searchType) || !Objects.equals(user, searchUser) || !Objects.equals(text, searchText) || byScore != searchByScore)
        {
            publishedFiles.clear();
            currentQuery = null;
            pagesQueried.clear();
            pendingQueries.clear();
            totalResults = -1;
        }

        searchText = text;
        searchUser = user;
        searchType = type;
        searchByScore = byScore;

        for (int i = start / results_count; i <= end / results_count; i++)
        {
            if (!pagesQueried.contains(i))
            {
                pagesQueried.add(i);
                pendingQueries.add(i);
            }
        }

        updateQueries();
    }

    public void updateQueries()
    {
        if (currentQuery == null && !pendingQueries.isEmpty())
        {
            int n = pendingQueries.peek();

            SteamUGCQuery q;
            if (searchUser == null)
                q = workshop.createQueryAllUGCRequest(searchByScore ? SteamUGC.UGCQueryType.RankedByVote : SteamUGC.UGCQueryType.RankedByPublicationDate, SteamUGC.MatchingUGCType.Items, handler.clientUtils.getAppID(), handler.clientUtils.getAppID(), n + 1);
            else
                q = workshop.createQueryUserUGCRequest(searchUser.getAccountID(), SteamUGC.UserUGCList.Published, SteamUGC.MatchingUGCType.Items, SteamUGC.UserUGCListSortOrder.CreationOrderDesc, handler.clientUtils.getAppID(), handler.clientUtils.getAppID(), n + 1);

            if (searchText != null)
                workshop.setSearchText(q, searchText);

            workshop.setMatchAnyTag(q, true);

            if (searchType != null)
                workshop.addRequiredTag(q, searchType);

            workshop.setAllowCachedResponse(q, 0);

            workshop.sendQueryUGCRequest(q);
            currentQuery = q;
        }
    }

    public void download(SteamUGCDetails d)
    {
        downloadingName = d.getTitle();
        downloadFile = d;
        currentDownloadVote = -2;
        workshop.getUserItemVote(d.getPublishedFileID());
        workshop.downloadItem(d.getPublishedFileID(), true);
    }

    public void delete(SteamUGCDetails d, String type)
    {
        this.uploadingType = type;
        workshop.deleteItem(d.getPublishedFileID());
    }

    public void updateDownload()
    {
        try
        {
            if (downloadFolder == null || downloadFolder.getSubfiles().isEmpty() || !(Game.screen instanceof ScreenWaiting))
                return;

            BaseFile f1 = Game.game.fileManager.getFile(downloadFolder.getSubfiles().get(0));
            f1.startReading();
            StringBuilder s = new StringBuilder();
            while (f1.hasNextLine())
            {
                s.append(f1.nextLine()).append("\n");
            }
            f1.stopReading();

            Screen s1 = Game.screen;
            if (s1 instanceof ScreenWaiting)
                s1 = ((ScreenWaiting) s1).previous;

            if (downloadFile.getTags().toLowerCase().contains("level"))
            {
                ScreenSaveLevel sc = new ScreenSaveLevel(downloadingName, s.toString(), s1);
                if (downloadFile.getDescription() != null)
                {
                    ArrayList<String> desc = Drawing.drawing.wrapText(downloadFile.getDescription(), 400, 12);
                    sc.description = new String[desc.size()];
                    desc.toArray(sc.description);
                }
                sc.workshopDetails = downloadFile;
                sc.votesUp = downloadFile.getVotesUp();
                sc.votesDown = downloadFile.getVotesDown();
                sc.download.posY -= sc.objHeight;
                sc.levelName.posY -= sc.objHeight;

                if (downloadFile.getOwnerID().equals(Game.steamNetworkHandler.playerID))
                    sc.showDelete = true;

                Level l = new Level(s.toString());
                l.preview = true;

                if (!l.isLarge())
                    l.loadLevel(sc);
                else
                    sc.queuedLevel = l;

                Game.screen = sc;
            }
            else if (downloadFile.getTags().toLowerCase().contains("crusade"))
            {
                ScreenCrusadePreview sc = new ScreenCrusadePreview(new Crusade(s.toString(), downloadFile.getTitle()), s1, false);
                if (downloadFile.getDescription() != null)
                {
                    ArrayList<String> desc = Drawing.drawing.wrapText(downloadFile.getDescription(), 400, 12);
                    sc.descriptionText = new String[desc.size()];
                    desc.toArray(sc.descriptionText);
                }
                sc.workshopDetails = downloadFile;
                sc.votesUp = downloadFile.getVotesUp();
                sc.votesDown = downloadFile.getVotesDown();

                if (downloadFile.getOwnerID().equals(Game.steamNetworkHandler.playerID))
                    sc.showDelete = true;

                Game.screen = sc;
            }

            downloadFolder = null;
        }
        catch (Exception e)
        {
            Game.exitToCrash(e);
        }
    }
}
