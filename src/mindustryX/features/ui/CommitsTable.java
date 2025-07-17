package mindustryX.features.ui;

import arc.*;
import arc.flabel.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Http.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustryX.features.ui.CommitsTable.CommitData.*;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

public class CommitsTable extends Table{
    private static final ObjectMap<String, TextureRegion> AVATAR_CACHE = new ObjectMap<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static float stroke = 1.5f;

    // commits sorted by date
    private final Seq<CommitData> commitsData = new Seq<>();

    public String repo;
    private final Table commitsTable = new Table();

    public CommitsTable(String repo){
        this.repo = repo;
    }

    @SuppressWarnings("unchecked")
    public CommitsTable update(){
        if(children.isEmpty()){
            setup();
        }
        setLoading(commitsTable);

        HttpRequest request = Http.get(Vars.ghApi + "/repos/" + repo + "/commits");
        request.header("Accept", "application/vnd.github+json");
        request.header("User-Agent", "TinyLake");

        request.error(e -> Core.app.post(() -> {
            Vars.ui.showException(e);
            setLoadFailed(commitsTable);
        }));

        request.submit(resp -> {
            String result = resp.getResultAsString();
            Core.app.post(() -> {
                Seq<CommitData> data = new Json().fromJson(Seq.class, CommitData.class, result);
                update(data);
            });
        });
        return this;
    }

    private void update(Seq<CommitData> data){
        commitsData.set(data);

        // no author?
        commitsData.removeAll(commitData -> commitData.commit.author == null);
        Comparator<CommitData> comparator = Comparator.nullsLast(Structs.comparing(c -> c.commit.author.getDate()));
        commitsData.sort(comparator.reversed());

        rebuildCommitsTable();
    }

    private void setup(){
        table(top -> {
            top.defaults().left();
            top.add(repo).style(Styles.outlineLabel).pad(4f);
            top.add("近期更新").color(Pal.lightishGray);
        }).padBottom(16f).padTop(8f).growX();

        row();

        pane(t -> {
            t.add(commitsTable).minHeight(200f).grow();
        }).grow();
    }

    private void rebuildCommitsTable(){
        commitsTable.clearChildren();

        commitsTable.image().color(color).width(stroke).growY();
        Table right = commitsTable.table().get();

        LocalDateTime lastDate = null;
        for(CommitData data : commitsData){
            LocalDateTime date = data.commit.author.getDate();

            // split by 1d
            if(date != null && (lastDate == null || !sameDay(lastDate, date))){
                right.table(timeSplit -> {
                    timeSplit.image().color(color).width(8f).height(stroke);
                    timeSplit.add(date.format(DATE_FORMATTER)).color(color).padLeft(8f).padRight(8f);
                    timeSplit.image().color(color).height(stroke).padRight(8f).growX();
                }).padTop(lastDate == null ? 0f : 16f).padBottom(8f).growX();
                right.row();

                lastDate = date;
            }

            right.table(commitInfo -> {
                setupCommitInfo(commitInfo, data);
            }).minWidth(400f).padLeft(16f).growX();

            right.row();
        }
    }

    private void setupCommitInfo(Table t, CommitData data){
        Commit commit = data.commit;
        Author author = data.author;
        LocalDateTime date = commit.author.getDate();
        LocalDateTime now = LocalDateTime.now();

        String[] split = commit.message.split("\n");
        t.table(left -> {
            left.defaults().left();

            Cell<?> topCell = left.table(top -> {
                top.add(split[0] + (split.length > 1 ? "..." : "")).style(Styles.outlineLabel).minWidth(350f).wrap().expandX().left();
                if(split.length > 1){
                    top.image(Icon.infoCircleSmall).pad(4f);
                }
            }).growX();
            if(split.length > 1){
                topCell.tooltip(commit.message, true);
            }

            left.row();

            left.table(bottom -> {
                bottom.defaults().left();
                bottom.image(() -> getAvatar(author.login, author.avatar_url)).pad(8f).size(Vars.iconMed);
                bottom.add(author.login).style(Styles.outlineLabel).color(Pal.lightishGray).padLeft(4f);
                bottom.add(date == null ? "unknown" : formatRelativeTime(date, now)).color(Pal.lightishGray).padLeft(4f);
            });
        });

        t.add().growX();

        t.table(right -> {
            right.defaults().size(Vars.iconMed).right();
            right.button(Icon.linkSmall, Styles.cleari, () -> Core.app.openURI(data.html_url));
        }).fillY();
    }

    private static TextureRegion getAvatar(String login, String url){
        if(!AVATAR_CACHE.containsKey(login)){
            // get once
            AVATAR_CACHE.put(login, Core.atlas.find("nomap"));

            Http.get(url, res -> {
                Pixmap pix = new Pixmap(res.getResult());
                Core.app.post(() -> {
                    try{
                        var tex = new Texture(pix);
                        tex.setFilter(TextureFilter.linear);
                        AVATAR_CACHE.put(login, new TextureRegion(tex));
                        pix.dispose();
                    }catch(Exception e){
                        Log.err(e);
                    }
                });
            }, err -> {
                // if error occurs, retry 2s later
                Time.run(2 * 1000, () -> AVATAR_CACHE.remove(login));
            });
        }

        return AVATAR_CACHE.get(login);
    }

    private static String formatRelativeTime(LocalDateTime from, LocalDateTime to) {
        long diffNano = Duration.between(from, to).toNanos();

        long seconds = TimeUnit.NANOSECONDS.toSeconds(diffNano);
        long minutes = TimeUnit.NANOSECONDS.toMinutes(diffNano);
        long hours = TimeUnit.NANOSECONDS.toHours(diffNano);
        long days = TimeUnit.NANOSECONDS.toDays(diffNano);

        if (seconds < 60) {
            return Core.bundle.format("commit.justNow");
        } else if (minutes < 60) {
            return Core.bundle.format("commit.minutesAgo", minutes);
        } else if (hours < 24) {
            return Core.bundle.format("commit.hoursAgo", hours);
        } else if (days < 7) {
            return Core.bundle.format("commit.daysAgo", days);
        } else {
            Period period = Period.between(from.toLocalDate(), to.toLocalDate());

            if (period.getYears() > 0) {
                return Core.bundle.format("commit.yearsAgo", period.getYears());
            } else if (period.getMonths() > 0) {
                return Core.bundle.format("commit.monthsAgo", period.getMonths());
            } else {
                long weeks = days / 7;
                return Core.bundle.format("commit.weeksAgo", weeks);
            }
        }
    }

    private static boolean sameDay(LocalDateTime date1, LocalDateTime date2) {
        return date1.toLocalDate().isEqual(date2.toLocalDate());
    }

    private static void setLoading(Table table){
        table.clearChildren();
        table.add(new FLabel("@alphaLoading")).style(Styles.outlineLabel).expand().center();
    }

    private static void setLoadFailed(Table table){
        table.clearChildren();
        table.add(new FLabel("@alphaLoadFailed")).style(Styles.outlineLabel).expand().center();
    }

    public static class CommitData{
        public String html_url;
        public Commit commit;
        public @Nullable Author author;
        public @Nullable Author committer;

        @Override
        public String toString(){
            return "CommitsData{" +
            "html_url='" + html_url + '\'' +
            ", commit=" + commit +
            ", author=" + author +
            ", committer=" + committer +
            '}';
        }

        public static class Commit{
            public String message;
            public @Nullable GitUser author;

            @Override
            public String toString(){
                return "Commit{" +
                "message='" + message + '\'' +
                ", author=" + author +
                '}';
            }
        }

        public static class Author{
            public String login;
            public @Nullable String name;
            public @Nullable String email;

            public String avatar_url;
            public String html_url;

            @Override
            public String toString(){
                return "Author{" +
                "name='" + name + '\'' +
                ", login='" + login + '\'' +
                ", email='" + email + '\'' +
                ", avatar_url='" + avatar_url + '\'' +
                ", html_url='" + html_url + '\'' +
                '}';
            }
        }

        public static class GitUser {

            public String name;
            public String email;
            public String date;

            private transient LocalDateTime cacheDate;

            @Override
            public String toString(){
                return "GitUser{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", date='" + date + '\'' +
                '}';
            }

            public LocalDateTime getDate(){
                if(cacheDate != null) return cacheDate;
                try{
                    return cacheDate = LocalDateTime.parse(date, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                }catch(Exception e){
                    return null;
                }
            }
        }
    }
}
