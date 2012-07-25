// @java.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.inspection.abbrev;

import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Abbreviation rules container.
 *
 * @author @java.author
 * @version @java.version
 */
public class AbbreviationRules {
    /** File refresh frequency. */
    private static final int FILE_REFRESH_FREQ = 5000;
    
    /** Hardcoded abbreviation table if no abbreviation file can be found. */
    private static final String[][] ABBREV_TABLE = {
        {"address", "addr"},
        {"administration", "admin"},
        {"argument", "arg"},
        {"array", "arr"},
        {"attachment", "attach"},
        {"attributes", "attrs"},
        {"buffer", "buf"},
        {"certificate", "cert"},
        {"callable", "call"},
        {"char", "c"},
        {"channel", "ch"},
        {"class", "cls"},
        {"closure", "c"},
        {"collection", "col"},
        {"connection", "conn"},
        {"command", "cmd"},
        {"communication", "comm"},
        {"comparator", "comp"},
        {"condition", "cond"},
        {"config", "cfg"},
        {"context", "ctx"},
        {"control", "ctrl"},
        {"coordinator", "crd"},
        {"copy", "cp"},
        {"counter", "cntr"},
        {"count", "cnt"},
        {"current", "curr"},
        {"database", "db"},
        {"declare", "decl"},
        {"declaration", "decl"},
        {"default", "dflt"},
        {"delete", "del"},
        {"delimiter", "delim"},
        {"description", "desc"},
        {"descriptor", "descr"},
        {"destination", "dest"},
        {"directory", "dir"},
        {"event", "evt"},
        {"exception", "e"},
        {"execute", "exec"},
        {"expected", "exp"},
        {"externalizable", "ext"},
        {"frequency", "freq"},
        {"future", "fut"},
        {"group", "grp"},
        {"handler", "hnd"},
        {"header", "hdr"},
        {"implementation", "impl"},
        {"index", "idx"},
        {"initial", "init"},
        {"initialize", "init"},
        {"interface", "itf"},
        {"iterator", "iter"},
        {"listener", "lsnr"},
        {"local", "loc"},
        {"locale", "loc"},
        {"logger", "log"},
        {"loader", "ldr"},
        {"manager", "mgr"},
        {"message", "msg"},
        {"method", "mtd"},
        {"microkernel", "mk"},
        {"milliseconds", "ms"},
        {"multicast", "mcast"},
        {"mutex", "mux"},
        {"network", "net"},
        {"number", "num"},
        {"object", "obj"},
        {"package", "pkg"},
        {"parameter", "param"},
        {"permission", "perm"},
        {"permissions", "perms"},
        {"password", "pwd"},
        {"pattern", "ptrn"},
        {"policy", "plc"},
        {"predicate", "pred"},
        {"priority", "pri"},
        {"projection", "prj"},
        {"projections", "prjs"},
        {"property", "prop"},
        {"properties", "props"},
        {"protocol", "proto"},
        {"process", "proc"},
        {"query", "qry"},
        {"receive", "rcv"},
        {"recipient", "rcpt"},
        {"reference", "ref"},
        {"remove", "rmv"},
        {"removed", "rmv"},
        {"rename", "ren"},
        {"repository", "repo"},
        {"request", "req"},
        {"resource", "rsrc"},
        {"response", "res"},
        {"send", "snd"},
        {"sender", "snd"},
        {"serializable", "ser"},
        {"service", "srvc"},
        {"session", "ses"},
        {"sequence", "seq"},
        {"sibling", "sib"},
        {"socket", "sock"},
        {"source", "src"},
        {"specification", "spec"},
        {"strategy", "stgy"},
        {"string", "str"},
        {"system", "sys"},
        {"taxonomy", "tax"},
        {"timestamp", "ts"},
        {"token", "tok"},
        {"topology", "top"},
        {"unicast", "ucast"},
        {"value", "val"},
        {"version", "ver"},
        {"windows", "win"},
    };

    /** Map from common words to abbreviations. */
    private volatile Map<String, String> abbreviationMap = new ConcurrentHashMap<String, String>();

    /** File with abbreviation rules. */
    private File abbreviationFile;

    /** Initialization latch. */
    private CountDownLatch initLatch = new CountDownLatch(1);

    /** Singleton instance. */
    private static volatile AbbreviationRules instance;

    /** Init flag */
    private static AtomicBoolean initFlag = new AtomicBoolean();

    /** Singleton init latch. */
    private static CountDownLatch singletonInitLatch = new CountDownLatch(1);

    /**
     * Singleton factory.
     *
     * @param file Abbreviations file.
     * @return Instance of abbreviation rules.
     */
    public static AbbreviationRules getInstance(@Nullable File file) {
        try {
            if (initFlag.compareAndSet(false, true)) {
                instance = new AbbreviationRules(file);
    
                singletonInitLatch.countDown();
            }
            else
                singletonInitLatch.await();

            return instance;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException("Interrupted while waiting for instance initialization");
        }
    }

    /**
     * Creates abbreviation rules depending on whether rules file found or not.
     *
     * @param file Abbreviations file or {@code null} if internal rules should be used.
     */
    private AbbreviationRules(File file) {
        if (file == null) {
            for (String[] entry : ABBREV_TABLE) {
                assert entry.length == 2;

                abbreviationMap.put(entry[0], entry[1]);
            }

            initLatch.countDown();
        }
        else {
            abbreviationFile = file;

            Thread fileWatchThread = new Thread(new AbbreviationFileWatcher());

            fileWatchThread.setDaemon(true);

            fileWatchThread.start();
        }
    }

    /**
     * Performs lookup of name part in abbreviation table.
     *
     * @param namePart Name part to lookup.
     * @return Abbreviation for given name or {@code null} if there is no such abbreviation.
     */
    @Nullable public String getAbbreviation(String namePart) {
        try {
            if (initLatch.getCount() == 1)
                initLatch.await();

            return abbreviationMap.get(namePart.toLowerCase());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return null;
        }
    }

    /**
     * Thread that watches for abbreviation file changes.
     */
    private class AbbreviationFileWatcher implements Runnable {
        /** File load timestamp. */
        private long loadTs;

        @Override public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    loadAbbreviations();
    
                    if (initLatch.getCount() == 1)
                        initLatch.countDown();
    
                    Thread.sleep(FILE_REFRESH_FREQ);
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Checks for file modification timestamp and refreshes abbreviation rules if needed.
         */
        @SuppressWarnings("unchecked")
        private void loadAbbreviations() {
            if (abbreviationFile.lastModified() > loadTs) {
                loadTs = abbreviationFile.lastModified();

                InputStream input = null;

                try {
                    input = new FileInputStream(abbreviationFile);

                    Properties props = new Properties();

                    props.load(new BufferedReader(new InputStreamReader(input)));
                    
                    Map<String, String> refreshed = new ConcurrentHashMap<String, String>();

                    for (Map.Entry<Object, Object> entry : props.entrySet())
                        refreshed.put((String)entry.getKey(), (String)entry.getValue());

                    abbreviationMap = refreshed;
                }
                catch (IOException ignored) {
                    // Just leave the last state.
                }
                finally {
                    if (input != null) {
                        try {
                            input.close();
                        }
                        catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    }
}
