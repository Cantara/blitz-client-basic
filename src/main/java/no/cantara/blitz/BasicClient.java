package no.cantara.blitz;

import net.jini.admin.Administrable;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.transaction.TransactionException;
import net.jini.space.JavaSpace;
import org.dancres.blitz.remote.BlitzAdmin;
import org.dancres.blitz.remote.StatsAdmin;
import org.dancres.blitz.stats.Stat;
import org.slf4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Basic Client based on the code from Dan Cresswell See https://github.com/dancres/blitzjavaspaces/blob/master/src/test/java/org/dancres/blitz/remote/test/BasicTest.java
 *
 * The easy way to start Blitz: https://github.com/Cantara/blitz-docker
 *
 * Created by baardl on 10.06.15.
 */
public class BasicClient {
    private static final Logger log = getLogger(BasicClient.class);

    public void exec(JavaSpace mySpace) throws Exception {

        writeAndTake(mySpace);


        Object myProxy = ((Administrable) mySpace).getAdmin();

        org.dancres.jini.util.DiscoveryUtil.dumpInterfaces(myProxy);

        printStatistics(myProxy);

        createSnapshot(myProxy);
    }

    private void createSnapshot(Object myProxy) {
        if (myProxy instanceof BlitzAdmin) {
            log.debug("Triggering snapshot");

            try {
                ((BlitzAdmin) myProxy).requestSnapshot();
            } catch (Exception anE) {
                log.error("Failed to requestSnapshot", anE);
            }
        } else
            log.debug("Warning no BlitzAdmin found");
    }

    private void printStatistics(Object myProxy) throws RemoteException {
        if (myProxy instanceof StatsAdmin) {
            log.debug("Recovering stats");
            Stat[] myStats = ((StatsAdmin) myProxy).getStats();

            for (int i = 0; i < myStats.length; i++) {
                log.debug(String.valueOf(myStats[i]));
            }
        } else
            log.debug("Warning no stats interface found");
    }

    private void writeAndTake(JavaSpace mySpace) throws TransactionException, RemoteException, UnusableEntryException, InterruptedException {
        mySpace.write(new MinimalEntry("abcdef", new Integer(33)),
                null, Lease.FOREVER);

        log.info("Add entry " +String.valueOf(mySpace.read(new MinimalEntry(), null, 1000)));
        log.info("Take entry " +String.valueOf(mySpace.take(new MinimalEntry(), null, 1000)));
        log.info("Attempt take on deleted entry : " +String.valueOf(mySpace.take(new MinimalEntry(), null, 1000)));
    }

    public static void main(String args[]) {

//        if (System.getSecurityManager() == null)
//            System.setSecurityManager(new RMISecurityManager());

        JavaSpace space = null;

        try {
            LookupLocator ll = new LookupLocator("jini://localhost:4160");
            space = connectToSpace(ll);
            BasicClient basicClient = new BasicClient();

            basicClient.exec(space);

        } catch (Exception anE) {
            log.error("Failed to connect to the space.", anE);
        }
    }

    private static JavaSpace connectToSpace(LookupLocator ll) throws IOException, ClassNotFoundException {
        JavaSpace space = null;
        ServiceRegistrar sr = ll.getRegistrar();
        log.debug("Service Registrar: " + sr.getServiceID());
        ServiceTemplate template = new ServiceTemplate(null, new Class[] { JavaSpace.class }, new Entry[0]);
        ServiceMatches sms = sr.lookup(template, 10);
        if(0 < sms.items.length) {
            space = (JavaSpace) sms.items[0].service;

            log.trace("Found Space {}", space.toString());
        } else {
            log.debug("No Java Space found.");
        }
        return space;
    }

    public static class MinimalEntry implements Entry {
        public String theName;
        public Integer theValue;

        public MinimalEntry() {
        }

        public MinimalEntry(String aName, Integer aValue) {
            theName = aName;
            theValue = aValue;
        }

        @Override
        public String toString() {
            return "MinimalEntry{" +
                    "theName='" + theName + '\'' +
                    ", theValue=" + theValue +
                    '}';
        }
    }
}
