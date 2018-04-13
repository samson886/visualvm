/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.heapviewer.truffle.python;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.model.TextNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableView;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableViewColumn;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
class PythonItemsPlugin extends HeapViewPlugin {

    private final Heap heap;
    private PythonObject selected;

    private final TreeTableView objectsView;

    PythonItemsPlugin(HeapContext context, HeapViewerActions actions) {
        super("Items", "Items", Icons.getIcon(ProfilerIcons.NODE_FORWARD));

        heap = context.getFragment().getHeap();

        objectsView = new TreeTableView("python_objects_items", context, actions, TreeTableViewColumn.instancesMinimal(heap, false)) {
            @Override
            protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                if (selected != null) {
                    List<FieldValue> fields = selected.getItems();
                    
                    HeapViewerNode[] nodes = getNodes(fields, root, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
                    return nodes == null || nodes.length == 0 ? new HeapViewerNode[] { new TextNode("<no items>") } : nodes;
                }
                
                return new HeapViewerNode[] { new TextNode("<no object selected>") };
            }
        };
    }

    @Override
    protected JComponent createComponent() {
        return objectsView.getComponent();
    }

    protected HeapViewerNode[] getNodes(List<FieldValue> fields, HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        return PythonItemsProvider.getNodes(fields, parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
    }

    @Override
    protected void nodeSelected(HeapViewerNode node, boolean adjusting) {
        TruffleObject selectedObject = node == null ? null : HeapViewerNode.getValue(node, TruffleObject.DATA_TYPE, heap);
        PythonObject selectedPythonObject = selectedObject instanceof PythonObject ? (PythonObject)selectedObject : null;
        if (Objects.equals(selected, selectedPythonObject)) return;

        selected = selectedPythonObject;

        objectsView.reloadView();
    }

    @ServiceProvider(service = HeapViewPlugin.Provider.class, position = 200)
    public static class Provider extends HeapViewPlugin.Provider {

        @Override
        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (PythonHeapFragment.isPythonHeap(context)) {
                return new PythonItemsPlugin(context, actions);
            }
            return null;
        }
    }
}
