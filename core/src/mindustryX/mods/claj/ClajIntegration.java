package mindustryX.mods.claj;

import arc.Events;
import arc.func.Cons;
import arc.net.Client;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.struct.Seq;
import arc.util.Reflect;
import arc.util.Threads;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.EventType.ClientPreConnectEvent;
import mindustry.gen.Call;
import mindustry.io.TypeIO;
import mindustry.net.ArcNetProvider.PacketSerializer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ClajIntegration {
    private static final Seq<Client> clients = new Seq<>();
    private static NetListener serverListener = null;

    public static void load() {
        Events.run(EventType.HostEvent.class, ClajIntegration::clear);
        Events.run(ClientPreConnectEvent.class, ClajIntegration::clear);

        var provider = Reflect.get(Vars.net, "provider");
        if (Vars.steam) provider = Reflect.get(provider, "provider"); // thanks


        var server = Reflect.get(provider, "server");
        serverListener = Reflect.get(server, "dispatchListener");
    }

// region room management

    public static Client createRoom(String ip, int port, Cons<String> link, Runnable disconnected) throws IOException {
        var client = new Client(8192, 8192, new Serializer());
        Threads.daemon("CLaJ Room", client);

        client.addListener(new NetListener() {
            /** Used when creating redirectors.  */
            String key = null;

            @Override
            public void connected(Connection connection) {
                client.sendTCP("new");
            }

            @Override
            public void disconnected(Connection connection, DcReason reason) {
                disconnected.run();
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof String s) {
                    if (s.startsWith("CLaJ")) {
                        key = s;
                        link.get(key + '#' + ip + ':' + port);
                    } else if (s.equals("new")) {
                        try {
                            createRedirector(ip, port, key);
                        } catch (Exception ignored) {
                        }
                    } else Call.sendMessage(s);
                }
            }
        });

        client.connect(5000, ip, port, port);
        clients.add(client);

        return client;
    }

    public static void createRedirector(String ip, int port, String key) throws IOException {
        var client = new Client(8192, 8192, new Serializer());
        Threads.daemon("CLaJ Redirector", client);

        client.addListener(serverListener);
        client.addListener(new NetListener() {
            @Override
            public void connected(Connection connection) {
                client.sendTCP("host" + key);
            }
        });

        client.connect(5000, ip, port, port);
        clients.add(client);
    }

    public static void joinRoom(String ip, int port, String key, Runnable success) {
        Vars.logic.reset();
        Vars.net.reset();

        Vars.netClient.beginConnecting();
        Vars.net.connect(ip, port, () -> {
            if (!Vars.net.client()) return;
            success.run();

            var buffer = ByteBuffer.allocate(8192);
            buffer.put(Serializer.linkID);
            TypeIO.writeString(buffer, "join" + key);

            buffer.limit(buffer.position()).position(0);
            Vars.net.send(buffer, true);
        });
    }

    private static void clear() {
        clients.each(Client::close);
        clients.clear();
    }

// endregion


    static class Serializer extends PacketSerializer {
        public static final byte linkID = -3;

        public void write(ByteBuffer buffer, Object object) {
            if (object instanceof String s) {
                buffer.put(linkID);
                TypeIO.writeString(buffer, s);
            } else super.write(buffer, object);
        }

        public Object read(ByteBuffer buffer) {
            if (buffer.get() == linkID) return TypeIO.readString(buffer);

            buffer.position(buffer.position() - 1);
            return super.read(buffer);
        }
    }
}