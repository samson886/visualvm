/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk.oql.ui;

import java.awt.BorderLayout;
import java.awt.Color;
<<<<<<< /home/hanz/Hanz/Dev/trunk/profiler/src/org/netbeans/modules/profiler/heapwalk/oql/ui/OQLEditor.java.orig.70905313
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
||||||| /tmp/OQLEditor.java~base.fkQOvu
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
=======
import javax.swing.BorderFactory;
>>>>>>> /tmp/OQLEditor.java~other.SPfNkT
import javax.swing.JEditorPane;
import javax.swing.JPanel;
<<<<<<< /home/hanz/Hanz/Dev/trunk/profiler/src/org/netbeans/modules/profiler/heapwalk/oql/ui/OQLEditor.java.orig.70905313
||||||| /tmp/OQLEditor.java~base.fkQOvu
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.heapwalk.OQLController;
=======
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
>>>>>>> /tmp/OQLEditor.java~other.SPfNkT
import org.netbeans.modules.profiler.heapwalk.oql.OQLEngine;
import org.netbeans.modules.profiler.spi.OQLEditorImpl;
import org.openide.util.Lookup;
/**
 *
 * @author Jaroslav Bachorik
 */
public class OQLEditor extends JPanel {
<<<<<<< /home/hanz/Hanz/Dev/trunk/profiler/src/org/netbeans/modules/profiler/heapwalk/oql/ui/OQLEditor.java.orig.70905313

    public static final String VALIDITY_PROPERTY = "validity"; // NOI18N
||||||| /tmp/OQLEditor.java~base.fkQOvu
    public static final String VALIDITY_PROPERTY = "validity"; // NOI18N
=======
>>>>>>> /tmp/OQLEditor.java~other.SPfNkT

    private JEditorPane queryEditor = null;
    final private OQLEngine engine;


    public OQLEditor(OQLEngine engine) {
        this.engine = engine;
        init();
    }

    
    private void init() {
        OQLEditorImpl impl = Lookup.getDefault().lookup(OQLEditorImpl.class);
        if (impl != null) {
            queryEditor = impl.getEditorPane();
            queryEditor.getDocument().putProperty(OQLEngine.class, engine);
<<<<<<< /home/hanz/Hanz/Dev/trunk/profiler/src/org/netbeans/modules/profiler/heapwalk/oql/ui/OQLEditor.java.orig.70905313
            queryEditor.addPropertyChangeListener(OQLEditorImpl.VALIDITY_PROPERTY,
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        setValidScript((Boolean)evt.getNewValue());
                    }
                });
||||||| /tmp/OQLEditor.java~base.fkQOvu
            queryEditor.addPropertyChangeListener(OQLEditorImpl.VALIDITY_PROPERTY, new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    setValidScript((Boolean)evt.getNewValue());
                }
            });
=======
>>>>>>> /tmp/OQLEditor.java~other.SPfNkT
        } else {
            queryEditor = new JEditorPane("text/x-oql", ""); // NOI18N
        }

        queryEditor.setOpaque(isOpaque());
        queryEditor.setBackground(getBackground());

        setLayout(new BorderLayout());
        add(queryEditor, BorderLayout.CENTER);
    }

<<<<<<< /home/hanz/Hanz/Dev/trunk/profiler/src/org/netbeans/modules/profiler/heapwalk/oql/ui/OQLEditor.java.orig.70905313
    
    public boolean isValidScript() {
        return validityFlag;
    }

    public void setScript(String script) {
        queryEditor.setText(script);
    }

||||||| /tmp/OQLEditor.java~base.fkQOvu
    public boolean isValidScript() {
        return validityFlag;
    }

=======
>>>>>>> /tmp/OQLEditor.java~other.SPfNkT
    public String getScript() {
        return queryEditor.getText();
    }
<<<<<<< /home/hanz/Hanz/Dev/trunk/profiler/src/org/netbeans/modules/profiler/heapwalk/oql/ui/OQLEditor.java.orig.70905313

    private void setValidScript(boolean value) {
        boolean oldValue = validityFlag;
        validityFlag = value;
        firePropertyChange(OQLEditor.VALIDITY_PROPERTY, oldValue, value);
    }


    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (queryEditor != null)
            queryEditor.setBackground(bg);
    }

    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);
        if (queryEditor != null)
            queryEditor.setOpaque(isOpaque);
    }

    public void requestFocus() {
        queryEditor.requestFocus();
    }

||||||| /tmp/OQLEditor.java~base.fkQOvu

    private void setValidScript(boolean value) {
        boolean oldValue = validityFlag;
        validityFlag = value;
        firePropertyChange(OQLEditor.VALIDITY_PROPERTY, oldValue, value);
    }
=======
>>>>>>> /tmp/OQLEditor.java~other.SPfNkT
}
