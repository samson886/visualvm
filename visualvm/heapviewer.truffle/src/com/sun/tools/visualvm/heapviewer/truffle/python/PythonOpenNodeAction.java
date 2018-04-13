/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleOpenNodeActionProvider;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleTypeNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.NodeObjectsView;
import org.netbeans.lib.profiler.heap.Instance;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNodeAction.Provider.class)
public class PythonOpenNodeAction  extends TruffleOpenNodeActionProvider<PythonObject, PythonType> {
    
    @Override
    public boolean supportsView(HeapContext context, String viewID) {
        return PythonHeapFragment.isPythonHeap(context);
    }
    
    @Override
    protected boolean supportsNode(HeapViewerNode node) {
        return node instanceof PythonNodes.PythonNode;
    }
    
    @Override
    protected boolean isLanguageObject(Instance instance) {
        return PythonObject.isPythonObject(instance);
    }
    
    @Override
    protected PythonObject createObject(Instance instance) {
        return new PythonObject(instance);
    }
    
    @Override
    protected PythonType getType(String name, HeapContext context) {
        return PythonHeapFragment.fromContext(context).getType(name, null);
    }

    @Override
    protected TruffleTypeNode createTypeNode(PythonType type) {
        return new PythonNodes.PythonTypeNode(type);
    }

    @Override
    protected NodeObjectsView createView(HeapViewerNode node, HeapContext context, HeapViewerActions actions) {
        return new PythonObjectView(node, context, actions);
    }
    
}
