package dev.mahdi.chat.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "signals.redis.enabled", havingValue = "true", matchIfMissing = true)
public class SignalRedisBridge implements SmartLifecycle {
  public static final String CHANNEL = "messenger:signals";
  private static final Logger log = LoggerFactory.getLogger(SignalRedisBridge.class);
  private final String host; private final int port; private final ObjectMapper json; private final SignalFeedState feed; private final SignalSocketHandler sockets;
  private volatile boolean running; private Socket publishSocket; private BufferedInputStream publishInput; private BufferedOutputStream publishOutput; private Socket subscribeSocket;

  SignalRedisBridge(@Value("${spring.data.redis.host:localhost}") String host, @Value("${spring.data.redis.port:6379}") int port,
                    ObjectMapper json, SignalFeedState feed, SignalSocketHandler sockets) {
    this.host=host; this.port=port; this.json=json; this.feed=feed; this.sockets=sockets;
  }
  public synchronized void publish(SignalUpdate update) {
    try { ensurePublisher(); writeCommand(publishOutput,"PUBLISH",CHANNEL,json.writeValueAsString(update)); readReply(publishInput); }
    catch(Exception error) { closePublisher(); markDegraded(); log.warn("Signal persisted but Redis publication failed: {}",error.getMessage()); }
  }
  @Override public void start() { running=true; Thread.ofPlatform().daemon().name("signal-redis-subscriber").start(this::subscribeLoop); }
  @Override public void stop() { running=false; close(subscribeSocket); closePublisher(); }
  @Override public boolean isRunning() { return running; }

  private void subscribeLoop() {
    while(running) {
      try(Socket socket=new Socket(host,port)) {
        subscribeSocket=socket; var input=new BufferedInputStream(socket.getInputStream()); var output=new BufferedOutputStream(socket.getOutputStream());
        writeCommand(output,"SUBSCRIBE",CHANNEL); readReply(input); markLive();
        while(running) { Object reply=readReply(input); if(reply instanceof Object[] parts && parts.length==3 && "message".equals(parts[0])) sockets.broadcastUpdate(json.readValue((String)parts[2],SignalUpdate.class)); }
      } catch(Exception error) { if(running){ markDegraded(); sleepBeforeRetry(); } }
    }
  }
  private void ensurePublisher() throws IOException { if(publishSocket!=null && publishSocket.isConnected() && !publishSocket.isClosed()) return; publishSocket=new Socket(host,port); publishInput=new BufferedInputStream(publishSocket.getInputStream()); publishOutput=new BufferedOutputStream(publishSocket.getOutputStream()); }
  private void markLive(){if(!feed.get().equals("LIVE")){feed.live();sockets.broadcastStatus();}}
  private void markDegraded(){if(!feed.get().equals("DEGRADED")){feed.degraded();sockets.broadcastStatus();}}
  private static void sleepBeforeRetry(){try{Thread.sleep(1000);}catch(InterruptedException error){Thread.currentThread().interrupt();}}
  private synchronized void closePublisher(){close(publishSocket);publishSocket=null;publishInput=null;publishOutput=null;}
  private static void close(Socket socket){if(socket!=null)try{socket.close();}catch(IOException ignored){}}
  private static void writeCommand(BufferedOutputStream output,String...args)throws IOException{output.write(("*"+args.length+"\r\n").getBytes(StandardCharsets.UTF_8));for(String arg:args){byte[] bytes=arg.getBytes(StandardCharsets.UTF_8);output.write(("$"+bytes.length+"\r\n").getBytes(StandardCharsets.UTF_8));output.write(bytes);output.write("\r\n".getBytes(StandardCharsets.UTF_8));}output.flush();}
  private static Object readReply(BufferedInputStream input)throws IOException{int type=input.read();if(type<0)throw new EOFException("Redis closed the connection");String line=readLine(input);if(type=='+'||type==':')return line;if(type=='$'){int length=Integer.parseInt(line);byte[] bytes=input.readNBytes(length);input.readNBytes(2);return new String(bytes,StandardCharsets.UTF_8);}if(type=='*'){Object[] values=new Object[Integer.parseInt(line)];for(int i=0;i<values.length;i++)values[i]=readReply(input);return values;}if(type=='-')throw new IOException("Redis error: "+line);throw new IOException("Unsupported Redis reply");}
  private static String readLine(BufferedInputStream input)throws IOException{ByteArrayOutputStream line=new ByteArrayOutputStream();int current;while((current=input.read())>=0){if(current=='\r'){if(input.read()!='\n')throw new IOException("Invalid Redis response");break;}line.write(current);}return line.toString(StandardCharsets.UTF_8);}
}
