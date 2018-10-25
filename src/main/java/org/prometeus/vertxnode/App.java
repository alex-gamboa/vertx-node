package org.prometeus.vertxnode;

import com.hazelcast.config.Config;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.reactivex.core.Vertx;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class App {

    public static void main(String[] args){

        Config clusterConfig = new Config();
        clusterConfig.getNetworkConfig().getInterfaces().setEnabled(true);

        getAllEnabledIPAddress()
            .forEach(address ->
                clusterConfig.getNetworkConfig().getInterfaces().addInterface(address)
            );

        ClusterManager manager = new HazelcastClusterManager(clusterConfig);
        VertxOptions options = new VertxOptions();

        options
            .setClustered(true)
            .setClusterManager(manager);

        Vertx.clusteredVertx(options, res -> {

            if (res.succeeded()){

                Vertx vertx = res.result();
                start(vertx);
            }else{

                System.out.println("failed: " + res.cause());
            }
        });
    }

    static void start(Vertx vertx){

        io.vertx.reactivex.core.eventbus.EventBus eventBus = vertx.eventBus();

        eventBus.consumer("grNode_SurpacNotFound", message -> {

            System.out.println("Surpac Not found");
        });

        eventBus.consumer("grNode_ProcessComplete", message -> {

            System.out.println("Process Complete");
        });

        JsonObject json = new JsonObject();
        json
            .put("name","alex")
            .put("lastName","Gamboa")
            .put("age","36")
            .put("body","this is the surpac script text")
            .put("parameters",new JsonArray().add("1").add("2").add("3"));



        eventBus
            .publish(
                "grHost_executeExtract",
                json);

        System.out.println("clustered event bus ready.");
    }

    static List<String> getAllEnabledIPAddress() {

        List<String> result = new ArrayList<String>();

        try {
            for(NetworkInterface ifc : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if(ifc.isUp()) {
                    for(InetAddress addr : Collections.list(ifc.getInetAddresses())) {
                        if(isValidIP(cleanIP(addr.toString())))
                            result.add(cleanIP(addr.toString()));
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return result;
    }

    static boolean isValidIP (String ip) {

        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    static String cleanIP (String ip){
        return ip.substring(1, ip.length());
    }
}
