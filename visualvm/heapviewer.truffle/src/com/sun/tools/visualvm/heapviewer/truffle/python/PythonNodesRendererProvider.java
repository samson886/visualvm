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
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerRenderer;
import java.util.Map;
import org.netbeans.lib.profiler.heap.Heap;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author thurka
 */
@ServiceProvider(service=HeapViewerRenderer.Provider.class)
public class PythonNodesRendererProvider extends HeapViewerRenderer.Provider {

    public boolean supportsView(HeapContext context, String viewID) {
        return true;
    }

    public void registerRenderers(Map<Class<? extends HeapViewerNode>, HeapViewerRenderer> renderers, HeapContext context) {
        Heap heap = context.getFragment().getHeap();

        renderers.put(PythonObjectNode.class, new PythonObjectNode.Renderer(heap));
        
        renderers.put(PythonTypeNode.class, new PythonObjectsContainer.Renderer());

        renderers.put(PythonObjectsContainer.class, new PythonObjectsContainer.Renderer());

        renderers.put(PythonObjectFieldNode.class, new PythonObjectFieldNode.Renderer(heap));

        renderers.put(PythonObjectReferenceNode.class, new PythonObjectReferenceNode.Renderer(heap));
    }

}