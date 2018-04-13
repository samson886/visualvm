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

import com.sun.tools.visualvm.heapviewer.java.PrimitiveNode;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.truffle.TerminalJavaNodes;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.ui.UIThresholds;
import com.sun.tools.visualvm.heapviewer.utils.NodesComputer;
import com.sun.tools.visualvm.heapviewer.utils.ProgressIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.SortOrder;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = HeapViewerNode.Provider.class, position = 300)
public class PythonItemsProvider extends HeapViewerNode.Provider {

    public String getName() {
        return "items";
    }

    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("python_");
    }

    public boolean supportsNode(HeapViewerNode parent, Heap heap, String viewID) {
        if (parent instanceof PythonNodes.PythonNode && !(parent instanceof PythonNodes.PythonObjectReferenceNode || parent instanceof PythonNodes.PythonObjectAttributeReferenceNode)) {
            TruffleObject object = HeapViewerNode.getValue(parent, TruffleObject.DATA_TYPE, heap);
            PythonObject pyobject = object instanceof PythonObject ? (PythonObject)object : null;
            if (pyobject != null) {
                if (getRawFields(pyobject).isEmpty()) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public HeapViewerNode[] getNodes(HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        return getNodes(getFields(parent, heap), parent, heap, viewID, viewFilter, dataTypes, sortOrders, progress);
    }

    static HeapViewerNode[] getNodes(List<FieldValue> fields, HeapViewerNode parent, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
        if (fields == null) return null;

        NodesComputer<Integer> computer = new NodesComputer<Integer>(fields.size(), UIThresholds.MAX_INSTANCE_FIELDS) {
            protected boolean sorts(DataType dataType) {
                return !DataType.COUNT.equals(dataType);
            }
            protected HeapViewerNode createNode(Integer index) {
                return PythonItemsProvider.createNode(fields.get(index), heap);
            }
            protected ProgressIterator<Integer> objectsIterator(int index, Progress progress) {
                Iterator<Integer> iterator = integerIterator(index, fields.size());
                return new ProgressIterator(iterator, index, false, progress);
            }
            protected String getMoreNodesString(String moreNodesCount)  {
                return "<another " + moreNodesCount + " items left>";
            }
            protected String getSamplesContainerString(String objectsCount)  {
                return "<sample " + objectsCount + " items>";
            }
            protected String getNodesContainerString(String firstNodeIdx, String lastNodeIdx)  {
                return "<items " + firstNodeIdx + "-" + lastNodeIdx + ">";
            }
        };

        return computer.computeNodes(parent, heap, viewID, null, dataTypes, sortOrders, progress);
    }


    protected List<FieldValue> getFields(HeapViewerNode parent, Heap heap) {
        TruffleObject object = parent == null ? null : HeapViewerNode.getValue(parent, TruffleObject.DATA_TYPE, heap);
        PythonObject pyobject = object instanceof PythonObject ? (PythonObject)object : null;
        if (pyobject == null) return null;

        List<FieldValue> fields = new ArrayList(getRawFields(pyobject));

        Iterator<FieldValue> fieldsIt = fields.iterator();
        while (fieldsIt.hasNext())
            if (!displayField(fieldsIt.next()))
                fieldsIt.remove();

        return fields;
    }

    List<FieldValue> getRawFields(PythonObject pyobject) {
        return pyobject.getItems();
    }

    private boolean displayField(FieldValue field) {
        // display primitive fields
        if (!(field instanceof ObjectFieldValue)) return true;

        Instance instance = ((ObjectFieldValue)field).getInstance();

        // display null fields
        if (instance == null) return true;

        // display DynamicObject fields
        if (PythonObject.isPythonObject(instance)) return true;

        // display primitive arrays
        if (instance instanceof PrimitiveArrayInstance) return true;

        String className = instance.getJavaClass().getName();

        if (className.startsWith("java.lang.") ||
            className.startsWith("com.oracle.graal.python.runtime.datatype."))
            return true;

        return false;
    }

    private static HeapViewerNode createNode(FieldValue field, Heap heap) {
        if (field instanceof ObjectFieldValue) {
            Instance instance = ((ObjectFieldValue)field).getInstance();
            if (PythonObject.isPythonObject(instance)) {
                PythonObject object = new PythonObject(instance);
                return new PythonNodes.PythonObjectFieldNode(new PythonObject(instance), object.getType(heap), field);
            } else {
                return new TerminalJavaNodes.Field((ObjectFieldValue)field, false);
            }
        } else {
            return new PrimitiveNode.Field(field);
        }
    }

}
