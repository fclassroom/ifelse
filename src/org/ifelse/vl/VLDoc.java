/*
 * Copyright 1999-2019 fclassroom Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ifelse.vl;

import com.intellij.openapi.project.Project;
import org.ifelse.IEAppLoader;
import org.ifelse.RP;
import org.ifelse.model.MDoc;
import org.ifelse.model.MFlowPoint;
import org.ifelse.model.MProject;
import org.ifelse.utils.IconFactory;
import org.ifelse.utils.Log;
import org.ifelse.utils.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VLDoc extends JPanel implements DropTargetListener, MouseListener, MouseMotionListener, KeyListener {

    public String title;
    public String flowid;
    List<VLItem> eles = new ArrayList<VLItem>();
    VLItem item_focus;

    VLLine line_new;

    VListener listener;
    private float scale = 1.0f;
    private boolean import_flow_ids;

    public VLDoc(VListener listener) {

        this.listener = listener;
        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this);


        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);

        setFocusable(true);

    }

    @Override
    public void paint(Graphics g) {

        /*
        if( !g.getFont().getName().equals("Dialog") )
        {
            repaint();
            return;
        }
        */

        Graphics2D g2d = (Graphics2D) g;

        if( scale != 1.0f )
            g2d.scale(scale,scale);

        Font font = new Font(Font.DIALOG, Font.BOLD, 16);
        g.setFont(font);
        g2d.translate(0, 0);
        g2d.setColor(Color.white);

        // Log.i("vldoc width:%d height:%d  font:%s",getWidth(),getHeight(),g.getFont().getName());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        for (VLItem item : eles)
            item.paint(g2d);


    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public void drop(DropTargetDropEvent dtde) {

        try {

            Object value = dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
            Log.i("vldoc accept:%s", value);
            {
                dtde.dropComplete(true);
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

            }
            if (value instanceof MFlowPoint) {


                zoom();

                MFlowPoint mFlowPoint = (MFlowPoint) value;

                MProject mProject = IEAppLoader.getMProject(listener.project());
                VLPoint point = new VLPoint();
                point.id = mProject.getSequenceStr( listener.project() );

                point.setImage(IconFactory.createImage(RP.Path.getIconPath(listener.project(), mFlowPoint.icon)));

                point.x = dtde.getLocation().x;
                point.y = dtde.getLocation().y;

                point.flow_point_id = mFlowPoint.id;


                point.mproperties = mFlowPoint.copyProperties();

                eles.add(point);


                point.setFocus(true);


                repaint();


                listener.onDataChanged();

            }


        } catch (Exception e) {

            e.printStackTrace();
            return;

        }

        Point p = new Point(dtde.getLocation().x - 20, dtde.getLocation().y - 20);


    }

    public List<VLItem> getElements() {
        return eles;
    }


    int off_x, off_y;

    boolean datachanged;
    @Override
    public void mouseDragged(MouseEvent e) {

        if (item_focus != null) {

            if (e.getModifiers() == InputEvent.BUTTON1_MASK) {

                if (item_focus instanceof VLPoint) {
                    item_focus.setXY(e.getX() - item_focus.width / 2, e.getY() - item_focus.height / 2);
                    datachanged = true;
                }

                repaint();
            } else {

                if (item_focus instanceof VLPoint) {

                    if( line_new == null ) {
                        line_new = new VLLine();

                        line_new.id_from = item_focus.id;
                        line_new.point_from = item_focus;

                        eles.add(line_new);
                    }

                    line_new.move_cursor = e.getPoint();
                    repaint();
                }

            }
        }

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {


        switch (e.getKeyCode()){

            case KeyEvent.VK_BACK_SPACE :

                if( item_focus != null  ) {



                    if( !(item_focus instanceof VLLine) )
                        for(int i=eles.size()-1;i>-1;i--){

                            if( eles.get(i) instanceof VLLine ){

                                VLLine line = (VLLine) eles.get(i) ;

                                if( line.point_from == item_focus || line.point_to == item_focus ){

                                    eles.remove(line);

                                }


                            }

                        }



                    eles.remove(item_focus);

                    listener.onRemoved(item_focus);

                    listener.onDataChanged();

                    repaint();


                }

                break;
            case KeyEvent.VK_C:{

                if( Util.isMac() ){
                    if(  !e.isMetaDown() ){
                        return;
                    }
                }
                else{
                    if(! e.isControlDown() ){
                        return;
                    }
                }

                IEAppLoader.copy_item = item_focus.clone();


                Log.i("copy item commond + c");

            }
            break;
            case KeyEvent.VK_V:{


                if( Util.isMac() ){
                    if(  !e.isMetaDown() ){
                        return;
                    }
                }
                else{
                    if(! e.isControlDown() ){
                        return;
                    }
                }

                if( IEAppLoader.copy_item != null ){


                    String id = IEAppLoader.getMProject(listener.project()).getSequenceStr(listener.project());

                    VLItem item = IEAppLoader.copy_item.clone();

                    item.id = id;

                    if( point_pressed != null ) {

                        item.x = point_pressed.x;
                        item.y = point_pressed.y;

                    }

                    eles.add(item);

                    onFocusChanged(item);

                    listener.onDataChanged();

                    repaint();

                }


            }
            break;
            case KeyEvent.VK_I:{


                if( Util.isMac() ){
                    if(  !e.isMetaDown() ){
                        return;
                    }
                }
                else{
                    if(! e.isControlDown() ){
                        return;
                    }
                }


                if( import_flow_ids ) {

                    clearPointId();
                    repaint();
                    import_flow_ids = false;
                    Log.console(listener.project(),"清除执行log");

                }
                else {
                    clearPointId();
                    import_flow_ids = true;
                    if (listener != null) {

                        List<String> ids = getIds(Util.getSysClipboardText());

                        VLItem last = null;
                        for(String id:ids) {

                            for (VLItem item : eles) {

                                if (item.id.equals(id)) {
                                    item.is_run_point = true;

                                    if( last != null )
                                        last.next_run_point_id = id;

                                    last = item;
                                }
                            }
                        }


                        repaint();
                    }

                }




            }
            break;
            case KeyEvent.VK_UP:{


                if(  e.isAltDown() ){
                    scale += 0.1f;
                    repaint();
                    return;
                }

            }
            break;
            case KeyEvent.VK_DOWN:{

                if(  e.isAltDown() ){
                    scale -= 0.1f;
                    repaint();
                    return;
                }

            }
            break;




        }




    }

    public MDoc getMDoc() {

        MDoc mDoc = new MDoc();

        mDoc.title = title;
        mDoc.flowid = flowid;
        mDoc.items = eles;

        return mDoc;

    }


    public static interface VListener {

        Project project();

        void onFocus(VLItem item_focus);

        void onDoubleClick(VLItem item_focus);

        void onRemoved(VLItem item_focus);

        void onDataChanged();


    }


    //mouse event


    @Override
    public void mouseClicked(MouseEvent e) {

    }


    Point point_pressed;


    @Override
    public void mousePressed(MouseEvent e) {


        point_pressed = e.getPoint();

        requestFocus();


        VLItem temp = getFocus(e.getPoint(), null);

        onFocusChanged(temp);

        repaint();

    }

    private void setFocus(VLItem focus) {
        if (item_focus != null)
            item_focus.setFocus(false);
        item_focus = focus;
        if (item_focus != null)
            item_focus.setFocus(true);

    }


    public void onFocusChanged(VLItem temp) {


        if (temp != null && item_focus == temp) {

            return;
        }
        setFocus(temp);
        if (item_focus != null && listener != null) {
            listener.onFocus(item_focus);
        } else if (item_focus == null) {

            listener.onFocus(null);

        }


    }

    public VLItem getFocus(Point p, VLItem except) {


        //Point fixp = new Point();

        for (int i = eles.size() - 1; i > -1; i--) {

            VLItem item = eles.get(i);

            if (item.isPointIn(p) && item != except) {

                return item;

            }
        }
        return null;

    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if( item_focus != null && e.getModifiers() == InputEvent.BUTTON3_MASK )
        {

            if( line_new != null ){

                VLItem  item = getFocus(e.getPoint(),item_focus);

                if( item == null || item.isLine() ) {

                    eles.remove(line_new);
                }
                else{

                    line_new.id = IEAppLoader.getMProject(listener.project()).getSequenceStr(listener.project());
                    line_new.id_to = item.id;
                    line_new.point_to = item;
                    line_new.newDefProperty();
                    listener.onDataChanged();

                }


            }
            line_new = null;
            repaint();
            return;

        }

        if( e.getClickCount() == 2  ){


            if( scale != 1.0f ) {
                zoom();
                return;
            }
            if( item_focus != null &&  item_focus.isLine() ){
                repaint();
                return;
            }
            if( item_focus != null && listener != null ) {
                listener.onDoubleClick(item_focus);
                return;
            }

        }

        if( datachanged ) {
            listener.onDataChanged();
            return;
        }



    }

    public void zoom(){

        if( scale != 1.0f ) {
            scale = 1.0f;
            repaint();
        }

    }
    public void zoomin(){

            scale += 0.1f;
            repaint();

    }
    public void zoomout(){


            scale -= 0.1f;
            repaint();


    }



    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }


    public void clearPointId(){

        for(VLItem item:eles){

            item.is_run_point = false;

        }

    }

    public static List<String> getIds(String context) {


        Pattern pattern_field = Pattern.compile("point\\(\\d+\\)");

        Matcher matcher = pattern_field.matcher(context);
        List<String> result = new ArrayList();

        while (matcher.find()) {
            String field = matcher.group();


            String id = field.substring("point(".length(), field.length() - 1);
            result.add(id);
            System.out.println(field + "-" + id);

        }
        return result;

    }
}
