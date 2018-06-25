/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.sampler.truffle.memory;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.sampler.truffle.AbstractSamplerSupport;
import org.graalvm.visualvm.sampler.truffle.AbstractSamplerSupport.Refresher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.MemoryMXBean;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.jfluid.results.memory.SampledMemoryResultsSnapshot;
import org.graalvm.visualvm.sampler.truffle.cpu.ThreadInfoProvider;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class MemorySamplerSupport extends AbstractSamplerSupport {
    
    private static final Logger LOGGER = Logger.getLogger(ThreadInfoProvider.class.getName());
    private static String AGENT_PATH = "modules/ext/stagent.jar";   // NOI18N

    private final Application application;
    
    private final MemoryMXBean memoryBean;
    private final HeapDumper heapDumper;
    private final SnapshotDumper snapshotDumper;
    private ObjectName truffleObjectName;
    private MBeanServerConnection conn;
    
    private java.util.Timer processor;
    
    private Timer heapTimer;
    private Refresher heapRefresher;
    private MemoryView heapView;

    private DataViewComponent.DetailsView[] detailsViews;
    
    public MemorySamplerSupport(Application application, Jvm jvm, boolean hasPermGen, MemoryMXBean memoryBean, SnapshotDumper snapshotDumper, HeapDumper heapDumper) {
        this.application = application;
        
        this.memoryBean = memoryBean;
        this.heapDumper = heapDumper;
        this.snapshotDumper = snapshotDumper;
    }
    
    
    public DataViewComponent.DetailsView[] getDetailsView() {
        if (detailsViews == null) {
            initialize();
            detailsViews = createViews();
        }
        heapView.initSession();
        return detailsViews.clone();
    }
    
    public boolean startSampling(ProfilingSettings settings, int samplingRate, int refreshRate) {
//        heapTimer.start();
//        permgenTimer.start();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (heapView != null) heapView.starting();
            }
        });

        heapRefresher.setRefreshRate(samplingRate);
        if (heapView != null) {
            doRefreshImpl(heapTimer, heapView);
        }
        return true;
    }
    
    public synchronized void stopSampling() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (heapView != null) heapView.stopping();
            }
        });
        
        heapTimer.stop();
        if (heapView != null) {
            doRefreshImplImpl(snapshotDumper.lastHistogram, heapView);
        }
    }
    
    public synchronized void terminate() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (heapView != null) heapView.terminated();
            }
        });
    }
    
    
    private String initialize() {
        if (application.getState() != Stateful.STATE_AVAILABLE) {
            return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable"); // NOI18N
        }
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel == null) {
            return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_init_jmx"); // NOI18N
        }
        if (jmxModel.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
            return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_create_jmx"); // NOI18N
        }
        conn = jmxModel.getMBeanServerConnection();

        try {
            if (!checkandLoadJMX(application)) {
                return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_threads");
            }
            if (!isHeapHistogramEnabled()) {
                return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_stacktraces");
            }
        } catch (SecurityException e) {
            LOGGER.log(Level.INFO, "threadBean.getThreadInfo(ids, maxDepth) throws SecurityException for " + application, e); // NOI18N
            return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_threads"); // NOI18N
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "threadBean.getThreadInfo(ids, maxDepth) throws Throwable for " + application, t); // NOI18N
            return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_threads"); // NOI18N
        }
        int defaultRefresh = GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000;
        
        processor = getTimer();
        
        heapTimer = new Timer(defaultRefresh, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                heapRefresher.refresh();
            }
        });
        heapRefresher = new Refresher() {
            public final boolean checkRefresh() {
                if (!heapTimer.isRunning()) return false;
                return heapView.isShowing();
            }
            public final void doRefresh() {
                if (heapView.isShowing()) {
                    doRefreshImpl(heapTimer, heapView);
                }
            }
            public final void setRefreshRate(int refreshRate) {
                heapTimer.setDelay(refreshRate);
                heapTimer.setInitialDelay(refreshRate);
                heapTimer.restart();
            }
            public final int getRefreshRate() {
                return heapTimer.getDelay();
            }
        };
        return null;
    }
    
    private DataViewComponent.DetailsView[] createViews() {
        int detailIndex = 0;
        int detailsCount = 1;
        DataViewComponent.DetailsView[] details = new DataViewComponent.DetailsView[detailsCount];
        
        heapView = new MemoryView(application, heapRefresher, MemoryView.MODE_HEAP, memoryBean, snapshotDumper, heapDumper);
        details[detailIndex++] = new DataViewComponent.DetailsView(
                    NbBundle.getMessage(MemorySamplerSupport.class, "LBL_Heap_histogram"), // NOI18N
                    null, 10, heapView, null);
        return details;
    }
    
    private void doRefreshImpl(final Timer timer, final MemoryView... views) {
        if (!timer.isRunning() || (views.length == 1 && views[0].isPaused())) return;
        
        try {
            processor.schedule(new TimerTask() {
                public void run() {
                    try {
                        if (!timer.isRunning()) return;
                        doRefreshImplImpl(takeHeapHistogram(), views);
                    } catch (Exception e) {
                        terminate();
                    }
                }
            }, 0);
        } catch (Exception e) {
            terminate();
        }
    }

    private HeapHistogram takeHeapHistogram() {
        try {
            Map<String, Object>[] histo = heapHistogram();

            return new TruffleHeapHistogram(histo);
        } catch (InstanceNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MBeanException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ReflectionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private void doRefreshImplImpl(final HeapHistogram heapHistogram, final MemoryView... views) {
        if (heapHistogram != null)
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    snapshotDumper.lastHistogram = heapHistogram;
                    for (MemoryView view : views) view.refresh(heapHistogram);
                }
            });
    }
    
    Map<String, Object>[] heapHistogram() throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (Map[]) conn.invoke(truffleObjectName, "heapHistogram", null, null);
    }

    boolean isHeapHistogramEnabled() throws InstanceNotFoundException, MBeanException, IOException, ReflectionException, AttributeNotFoundException {
        return (boolean) conn.getAttribute(truffleObjectName, "HeapHistogramEnabled");
    }

    boolean checkandLoadJMX(Application app) throws MalformedObjectNameException, IOException, InterruptedException {
        truffleObjectName = new ObjectName("com.truffle:type=Threading");
        if (conn.isRegistered(truffleObjectName)) {
            return true;
        }
        if (loadAgent(app)) {
            for (int i = 0; i < 10; i++) {
                if (conn.isRegistered(truffleObjectName)) {
                    return true;
                }
                Thread.sleep(300);
            }
        }
        return conn.isRegistered(truffleObjectName);
    }

    boolean loadAgent(Application app) {
        String pid = String.valueOf(app.getPid());
        String agentPath = getAgentPath();

        LOGGER.warning("Agent " + agentPath);    // NOI18N
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            LOGGER.warning(vm.toString());
            vm.loadAgent(agentPath);
            vm.detach();
            LOGGER.warning("Agent loaded");    // NOI18N
            return true;
        } catch (AttachNotSupportedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (AgentLoadException ex) {
            Exceptions.printStackTrace(ex);
        } catch (AgentInitializationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    private String getAgentPath() {
        InstalledFileLocator loc = InstalledFileLocator.getDefault();
        File jar = loc.locate(AGENT_PATH, "org.graalvm.visualvm.sampler.truffle", false);   // NOI18N

        return jar.getAbsolutePath();
    }

    public static abstract class HeapDumper {
        public abstract void takeHeapDump(boolean openView);
    }
    
    public static abstract class SnapshotDumper {
        private volatile HeapHistogram lastHistogram;
        
        public abstract void takeSnapshot(boolean openView);
        
        public SampledMemoryResultsSnapshot createSnapshot(long time) {
            HeapHistogram histogram = lastHistogram;

            if (histogram != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
                DataOutputStream dos = new DataOutputStream(output);
                try {
                    SampledMemoryResultsSnapshot result = new SampledMemoryResultsSnapshot();
                    Set<HeapHistogram.ClassInfo> classes = histogram.getHeapHistogram();
                    
                    dos.writeInt(1);    // version
                    dos.writeLong(histogram.getTime().getTime()); // begin time
                    dos.writeLong(time); // taken time
                    dos.writeInt(classes.size());   // no of classes
                    for (HeapHistogram.ClassInfo info : classes) {
                        dos.writeUTF(info.getName());       // name
                        dos.writeLong(info.getBytes());     // total number of bytes
                    }
                    dos.writeBoolean(false); // no stacktraces
                    dos.writeInt(classes.size());   // no of classes
                    for (HeapHistogram.ClassInfo info : classes) {
                        dos.writeInt((int)info.getInstancesCount());     // number of instances
                    }
                    dos.close();
                    result.readFromStream(new DataInputStream(new ByteArrayInputStream(output.toByteArray())));
                    return result;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }

    private class TruffleHeapHistogram extends HeapHistogram {

        private long totalInstances;
        private long totalBytes;
        private final long time;
        private final Set<ClassInfo> classes;

        private TruffleHeapHistogram(Map<String, Object>[] heap) {
            time = System.currentTimeMillis();
            classes = new HashSet(heap.length);
            for (Map<String, Object> classInfo : heap) {
                ClassInfo info = new TruffleClassInfo(classInfo);

                totalInstances += info.getInstancesCount();
                totalBytes += info.getBytes();
                classes.add(info);
            }
        }

        @Override
        public Date getTime() {
            return new Date(time);
        }

        @Override
        public long getTotalInstances() {
            return totalInstances;
        }

        @Override
        public long getTotalBytes() {
            return totalBytes;
        }

        @Override
        public Set<ClassInfo> getHeapHistogram() {
            return Collections.unmodifiableSet(classes);
        }

        @Override
        public long getTotalHeapInstances() {
            return totalInstances;
        }

        @Override
        public long getTotalHeapBytes() {
            return totalBytes;
        }

        @Override
        public Set<ClassInfo> getPermGenHistogram() {
            return null;
        }

        @Override
        public long getTotalPerGenInstances() {
            return 0;
        }

        @Override
        public long getTotalPermGenHeapBytes() {
            return 0;
        }
    }

    private class TruffleClassInfo extends HeapHistogram.ClassInfo {

        private final String name;
        private final long instances;
        private final long bytes;

        private TruffleClassInfo(Map<String,Object> info) {
            name = info.get("language")+"."+info.get("name");
            instances = (Long) info.get("instancesCount");
            bytes = (Long) info.get("bytes");
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getInstancesCount() {
            return instances;
        }

        @Override
        public long getBytes() {
            return bytes;
        }
    }
}