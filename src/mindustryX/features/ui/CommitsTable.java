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

public class CommitsTable extends Table{
    private static final ObjectMap<String, TextureRegion> AVATAR_CACHE = new ObjectMap<>();
    private static final TextureRegion NOT_FOUND = Core.atlas.find("nomap");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final float STROKE = 1.5f;

    // commits sorted by date
    private final Seq<CommitData> commitsData = new Seq<>();

    public String repo;
    private final Table commitsTable = new Table();

    private boolean autoFetched = false;

    public CommitsTable(String repo){
        this.repo = repo;

        table(top -> {
            top.defaults().left();
            top.add(this.repo).style(Styles.outlineLabel).pad(4f);
            top.add("@commit.recentUpdates").color(Pal.lightishGray);
        }).padBottom(16f).padTop(8f).growX();

        row();

        pane(t -> t.add(commitsTable).minHeight(200f).grow()).grow();
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(!commitsTable.hasChildren() && !autoFetched){
            autoFetched = true;
            fetch();
        }
    }

    @SuppressWarnings("unchecked")
    public void fetch(){
        commitsTable.clearChildren();
        commitsTable.add(new FLabel("@alphaLoading")).style(Styles.outlineLabel).expand().center();

        HttpRequest request = Http.get(Vars.ghApi + "/repos/" + repo + "/commits");
        request.header("Accept", "application/vnd.github+json");
        request.header("User-Agent", "MindustryX");

        request.error(e -> Core.app.post(() -> {
            Vars.ui.showException(e);
            commitsTable.clearChildren();
            commitsTable.add(new FLabel("@alphaLoadFailed")).style(Styles.outlineLabel).expand().center();
        }));
        request.submit(resp -> {
            String result = resp.getResultAsString();
            Seq<CommitData> data = new Json().fromJson(Seq.class, CommitData.class, result);
            Core.app.post(() -> {
                if(data == null){
                    commitsTable.clearChildren();
                    commitsTable.add(new FLabel("@alphaLoadFailed")).style(Styles.outlineLabel).expand().center();
                    return;
                }

                commitsData.set(data);
                // no author?
                commitsData.removeAll(commitData -> commitData.commit.author == null);
                Comparator<CommitData> comparator = Comparator.nullsFirst(Structs.comparing(c -> c.commit.author.getDate()));
                commitsData.sort(comparator.reversed());

                rebuildCommitsTable();
            });
        });
    }

    private void rebuildCommitsTable(){
        commitsTable.clearChildren();

        commitsTable.image().color(color).width(STROKE).growY();
        Table right = commitsTable.table().get();

        LocalDateTime lastDate = null;
        for(CommitData data : commitsData){
            LocalDateTime date = data.commit.author.getDate();

            // split by 1d
            if(date != null && (lastDate == null || !lastDate.toLocalDate().isEqual(date.toLocalDate()))){
                right.table(timeSplit -> {
                    timeSplit.image().color(color).width(8f).height(STROKE);
                    timeSplit.add(date.format(DATE_FORMATTER)).color(color).padLeft(8f).padRight(8f);
                    timeSplit.image().color(color).height(STROKE).padRight(8f).growX();
                }).padTop(lastDate == null ? 0f : 16f).padBottom(8f).growX();
                right.row();

                lastDate = date;
            }

            right.table(commitInfo -> setupCommitInfo(commitInfo, data)).minWidth(400f).padLeft(16f).growX();

            right.row();
        }
    }

    private void setupCommitInfo(Table t, CommitData data){
        Commit commit = data.commit;
        Author author = data.author;

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
                bottom.image(getAvatar(author.login, author.avatar_url)).pad(8f).size(Vars.iconMed);
                bottom.add(author.login).style(Styles.outlineLabel).color(Pal.lightishGray).padLeft(4f);
            });
        });

        t.add().growX();

        t.table(right -> {
            right.defaults().size(Vars.iconMed).right();
            right.button(Icon.linkSmall, Styles.cleari, () -> Core.app.openURI(data.html_url));
        }).fillY();
    }

    private static TextureRegion getAvatar(String login, String url){
        TextureRegion region = AVATAR_CACHE.get(login, TextureRegion::new);
        if(region.texture == null){
            region.set(NOT_FOUND);
            Http.get(url, res -> {
                Pixmap pix = new Pixmap(res.getResult());
                Core.app.post(() -> {
                    try{
                        var tex = new Texture(pix);
                        tex.setFilter(TextureFilter.linear);
                        region.set(tex);
                    }catch(Exception e){
                        Log.err(e);
                    }

                    pix.dispose();
                });
            }, err -> {
                region.set(NOT_FOUND);
                Log.err(err);
            });
        }
        return region;
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
