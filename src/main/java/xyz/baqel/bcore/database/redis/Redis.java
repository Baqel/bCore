package xyz.baqel.bcore.database.redis;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import xyz.baqel.bcore.database.redis.handler.IncomingPacketHandler;
import xyz.baqel.bcore.database.redis.handler.PacketExceptionHandler;
import xyz.baqel.bcore.database.redis.listener.PacketListener;
import xyz.baqel.bcore.database.redis.listener.PacketListenerData;
import xyz.baqel.bcore.database.redis.packet.Packet;
import xyz.baqel.bcore.util.Chat;
import xyz.baqel.bcore.util.Chat.Level;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

@Getter
@RequiredArgsConstructor
public class Redis {
	
    private static JsonParser PARSER;
    private String channel;
    
    @Getter 
    private JedisPool jedisPool;
    @Getter 
    private JedisPubSub jedisPubSub;
    @Setter 
    private List<PacketListenerData> packetListeners;
    
    private Map<Integer, Class> idToType;
    private Map<Class, Integer> typeToId;
    
    static {
        Redis.PARSER = new JsonParser();
    }
    
    public Redis(String channel, String host, int port, String password) {
        this.packetListeners = new ArrayList<>();
        this.idToType = new HashMap<>();
        this.typeToId = new HashMap<>();
        this.channel = channel;
        this.packetListeners = new ArrayList<>();
        this.jedisPool = new JedisPool(host, port);
       
        if (password != null && !password.equals("")) {
            try (Jedis jedis = this.jedisPool.getResource()) {
                jedis.auth(password);
            }
        }
        
        this.setupPubSub();
    }
    
    public void sendPacket(Packet packet) {
        this.sendPacket(packet, null);
    }
    
    public void sendPacket(Packet packet, PacketExceptionHandler exceptionHandler) {
        try {
            JsonObject object = packet.serialize();
            if (object == null) {
                throw new IllegalStateException("Packet cannot generate null serialized data");
            }
            try (Jedis jedis = this.jedisPool.getResource()) {
                jedis.publish(this.channel, packet.id() + ";" + object.toString());
            }
        } catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.onException(e);
            }
        }
    }
    
    public Packet buildPacket(int id) {
        if (!this.idToType.containsKey(id)) {
            throw new IllegalStateException("A packet with that ID does not exist");
        }
        try {
            return (Packet) this.idToType.get(id).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not create new instance of packet type");
        }
    }
    
    public void registerPacket(Class clazz) {
        try {
            int id = (int) clazz.getDeclaredMethod("id", new Class[0]).invoke(clazz.newInstance(), null);
            if (this.idToType.containsKey(id) || this.typeToId.containsKey(clazz)) {
                throw new IllegalStateException("A packet with that ID has already been registered");
            }
            this.idToType.put(id, clazz);
            this.typeToId.put(clazz, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void registerListener(PacketListener packetListener) {
        for (Method method : packetListener.getClass().getDeclaredMethods()) {
            if (method.getDeclaredAnnotation(IncomingPacketHandler.class) != null) {
                Class packetClass = null;
                if (method.getParameters().length > 0 && Packet.class.isAssignableFrom(method.getParameters()[0].getType())) {
                    packetClass = method.getParameters()[0].getType();
                }
                if (packetClass != null) {
                    this.packetListeners.add(new PacketListenerData(packetListener, method, packetClass));
                }
            }
        }
    }
    
    private void setupPubSub() {
        this.jedisPubSub = new JedisPubSub() {
            
        	@Override
        	public void onMessage(String channel, String message) {
                if (channel.equalsIgnoreCase(Redis.this.channel)) {
                    try {
                        String[] args = message.split(";");
                        Integer id = Integer.valueOf(args[0]);
                        Packet packet = Redis.this.buildPacket(id);
                        if (packet != null) {
                            packet.deserialize(Redis.PARSER.parse(args[1]).getAsJsonObject());
                            for (PacketListenerData data : Redis.this.packetListeners) {
                                if (data.matches(packet)) {
                                    data.getMethod().invoke(data.getInstance(), packet);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Chat.log(Level.EXCEPTION, "&c[Redis] Failed to handle message.");
                        e.printStackTrace();
                    }
                }
            }
        };
        ForkJoinPool.commonPool().execute(() -> {
        	Jedis jedis = this.jedisPool.getResource();
            
        	try {
                jedis.subscribe(this.jedisPubSub, new String[] { this.channel });
            }
            finally {
                if (jedis != null) {
                	jedis.close();
                }
            }
        });
    }
}
