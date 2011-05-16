/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/gwt/client/dnd/Attic/CmsDNDHandler.java,v $
 * Date   : $Date: 2011/05/16 10:08:54 $
 * Version: $Revision: 1.15 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2011 Alkacon Software (http://www.alkacon.com)-
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.gwt.client.dnd;

import org.opencms.gwt.client.util.CmsDebugLog;
import org.opencms.gwt.client.util.CmsDomUtil;
import org.opencms.gwt.client.util.CmsMoveAnimation;
import org.opencms.gwt.client.util.CmsDomUtil.Style;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Drag and drop handler.<p>
 * 
 * @author Tobias Herrmann
 * 
 * @version $Revision: 1.15 $
 * 
 * @since 8.0.0
 */
public class CmsDNDHandler implements MouseDownHandler {

    /** The allowed drag and drop orientation. */
    public enum Orientation {
        /** Drag and drop in all directions. */
        ALL,
        /** Only horizontal drag and drop, the client-y position will be ignored. */
        HORIZONTAL,
        /** Only vertical drag and drop, the client-x position will be ignored. */
        VERTICAL
    }

    /**
     * Timer to schedule automated scrolling.<p>
     */
    protected class CmsScrollTimer extends Timer {

        /** The current scroll direction. */
        private Direction m_direction;

        /** Flag indicating if the scroll parent is the body element. */
        private boolean m_isBody;

        /** The element that should scrolled. */
        private Element m_scrollParent;

        /** The scroll speed. */
        private int m_scrollSpeed;

        /**
         * Constructor.<p>
         * 
         * @param scrollParent the element that should scrolled
         * @param scrollSpeed the scroll speed
         * @param direction the scroll direction
         */
        public CmsScrollTimer(Element scrollParent, int scrollSpeed, Direction direction) {

            m_scrollParent = scrollParent;
            m_scrollSpeed = scrollSpeed;
            m_isBody = m_scrollParent.getTagName().equalsIgnoreCase(CmsDomUtil.Tag.body.name());
            m_direction = direction;
        }

        /**
         * @see com.google.gwt.user.client.Timer#run()
         */
        @Override
        public void run() {

            int top, left;
            if (m_isBody) {
                top = Window.getScrollTop();
                left = Window.getScrollLeft();
            } else {
                top = m_scrollParent.getScrollTop();
                left = m_scrollParent.getScrollLeft();
            }
            Element element = getDragHelper();

            boolean abort = false;
            switch (m_direction) {
                case down:
                    top += m_scrollSpeed;
                    element.getStyle().setTop(
                        CmsDomUtil.getCurrentStyleInt(element, Style.top) + m_scrollSpeed,
                        Unit.PX);
                    break;
                case up:
                    if (top <= m_scrollSpeed) {
                        abort = true;
                        top = 0;
                        element.getStyle().setTop(CmsDomUtil.getCurrentStyleInt(element, Style.top) - top, Unit.PX);
                        break;
                    }
                    top -= m_scrollSpeed;
                    element.getStyle().setTop(
                        CmsDomUtil.getCurrentStyleInt(element, Style.top) - m_scrollSpeed,
                        Unit.PX);
                    break;
                case left:
                    if (left <= m_scrollSpeed) {
                        abort = true;
                        element.getStyle().setLeft(CmsDomUtil.getCurrentStyleInt(element, Style.left) - left, Unit.PX);
                        left = 0;
                        break;
                    }
                    left -= m_scrollSpeed;
                    element.getStyle().setLeft(
                        CmsDomUtil.getCurrentStyleInt(element, Style.left) - m_scrollSpeed,
                        Unit.PX);
                    break;
                case right:
                    left += m_scrollSpeed;
                    element.getStyle().setLeft(
                        CmsDomUtil.getCurrentStyleInt(element, Style.left) + m_scrollSpeed,
                        Unit.PX);
                    break;
                default:
                    break;

            }

            if (m_isBody) {
                Window.scrollTo(left, top);
            } else {
                m_scrollParent.setScrollLeft(left);
                m_scrollParent.setScrollTop(top);
            }
            if (abort) {
                clearScrollTimer();
            }
        }
    }

    /** Scroll direction enumeration. */
    protected enum Direction {
        /** Scroll direction. */
        down,

        /** Scroll direction. */
        left,

        /** Scroll direction. */
        right,

        /** Scroll direction. */
        up
    }

    /**
     * Drag and drop event preview handler.<p>
     * 
     * To be used while dragging.<p>
     */
    protected class DNDEventPreviewHandler implements NativePreviewHandler {

        /**
         * @see com.google.gwt.user.client.Event.NativePreviewHandler#onPreviewNativeEvent(com.google.gwt.user.client.Event.NativePreviewEvent)
         */
        public void onPreviewNativeEvent(NativePreviewEvent event) {

            if (!isDragging()) {
                // this should never happen, as the preview handler should be removed after the dragging stopped
                return;
            }
            Event nativeEvent = Event.as(event.getNativeEvent());
            switch (DOM.eventGetType(nativeEvent)) {
                case Event.ONMOUSEMOVE:
                    // dragging
                    onMove(nativeEvent);
                    break;
                case Event.ONMOUSEUP:
                    onUp(nativeEvent);
                    break;
                case Event.ONKEYDOWN:
                    if (nativeEvent.getKeyCode() == 27) {
                        cancel();
                    }
                    break;
                case Event.ONMOUSEWHEEL:
                    onMouseWheelScroll(nativeEvent);
                    break;
                default:
                    // do nothing
            }
            event.cancel();
            nativeEvent.preventDefault();
            nativeEvent.stopPropagation();
        }

    }

    /** Animation enabled flag. */
    private boolean m_animationEnabled = true;

    /** The mouse x position of the current mouse event. */
    private int m_clientX;

    /** The mouse y position of the current mouse event. */
    private int m_clientY;

    /** The Drag and drop controller. */
    private I_CmsDNDController m_controller;

    /** The current animation. */
    private CmsMoveAnimation m_currentAnimation;

    /** The current drop target. */
    private I_CmsDropTarget m_currentTarget;

    /** The x cursor offset to the dragged element. */
    private int m_cursorOffsetX;

    /** The y cursor offset to the dragged element. */
    private int m_cursorOffsetY;

    /** The draggable. */
    private I_CmsDraggable m_draggable;

    /** The dragging flag. */
    private boolean m_dragging;

    /** The drag helper. */
    private Element m_dragHelper;

    /** The drag and drop orientation. Default is <code>ALL</code>. */
    private Orientation m_orientation = Orientation.ALL;

    /** The placeholder. */
    private Element m_placeholder;

    /** The event preview handler. */
    private DNDEventPreviewHandler m_previewHandler;

    /** The preview handler registration. */
    private HandlerRegistration m_previewHandlerRegistration;

    /** Current scroll direction. */
    private Direction m_scrollDirection;

    /** Flag if automatic scrolling is enabled. */
    private boolean m_scrollEnabled = true;

    /** Scroll timer. */
    private Timer m_scrollTimer;

    /** The starting position absolute left. */
    private int m_startLeft;

    /** The starting position absolute top. */
    private int m_startTop;

    /** The registered drop targets. */
    private List<I_CmsDropTarget> m_targets;

    /** 
     * Constructor.<p> 
     * 
     * @param controller the drag and drop controller 
     **/
    public CmsDNDHandler(I_CmsDNDController controller) {

        m_targets = new ArrayList<I_CmsDropTarget>();
        m_previewHandler = new DNDEventPreviewHandler();
        m_controller = controller;
    }

    /**
     * Adds a drop target.<p>
     * 
     * @param target the target to add
     */
    public void addTarget(I_CmsDropTarget target) {

        m_targets.add(target);
    }

    /** 
     * Cancels the dragging process.<p>
     */
    public void cancel() {

        animateCancel(m_draggable, m_controller);
    }

    /**
     * Clears the drop target register.<p>
     */
    public void clearTargets() {

        m_targets.clear();
    }

    /**
     * Drops the draggable.<p>
     */
    public void drop() {

        // notifying controller, if false is returned, dropping will be canceled
        if (!m_controller.onBeforeDrop(m_draggable, m_currentTarget, this)) {
            cancel();
            return;
        }
        animateDrop(m_draggable, m_currentTarget, m_controller);
    }

    /**
     * Returns the drag and drop controller.<p>
     *
     * @return the drag and drop controller
     */
    public I_CmsDNDController getController() {

        return m_controller;
    }

    /**
     * Returns the current drop target.<p>
     * 
     * @return the current drop target
     */
    public I_CmsDropTarget getCurrentTarget() {

        return m_currentTarget;
    }

    /**
     * Returns the cursor offset x.<p>
     *
     * @return the cursor offset x
     */
    public int getCursorOffsetX() {

        return m_cursorOffsetX;
    }

    /**
     * Returns the cursor offset y.<p>
     *
     * @return the cursor offset y
     */
    public int getCursorOffsetY() {

        return m_cursorOffsetY;
    }

    /**
     * Returns the current draggable.<p>
     * 
     * @return the draggable
     */
    public I_CmsDraggable getDraggable() {

        return m_draggable;
    }

    /**
     * Returns the drag helper element.<p>
     * 
     * @return the drag helper
     */
    public Element getDragHelper() {

        return m_dragHelper;
    }

    /**
     * Returns the allowed drag and drop orientation.<p>
     *
     * @return the drag and drop orientation
     */
    public Orientation getOrientation() {

        return m_orientation;
    }

    /**
     * Returns the place holder element.<p>
     * 
     * @return the place holder element
     */
    public Element getPlaceholder() {

        return m_placeholder;
    }

    /**
     * Returns if the animation is enabled.<p>
     *
     * @return <code>true</code> if the animation is enabled
     */
    public boolean isAnimationEnabled() {

        return m_animationEnabled;
    }

    /**
     * Returns if a dragging process is taking place.<p>
     * 
     * @return <code>true</code> if the handler is currently dragging
     */
    public boolean isDragging() {

        return m_dragging;
    }

    /**
     * Returns if automated scrolling is enabled.<p>
     *
     * @return if automated scrolling is enabled
     */
    public boolean isScrollEnabled() {

        return m_scrollEnabled;
    }

    /**
     * @see com.google.gwt.event.dom.client.MouseDownHandler#onMouseDown(com.google.gwt.event.dom.client.MouseDownEvent)
     */
    public void onMouseDown(MouseDownEvent event) {

        if ((event.getNativeButton() != NativeEvent.BUTTON_LEFT) || m_dragging || (m_currentAnimation != null)) {
            // only act on left button down, ignore right click
            // also ignore if the dragging flag is still true or an animation is still running
            return;
        }
        Object source = event.getSource();
        if (!(source instanceof I_CmsDragHandle)) {
            // source is no drag handle, wrong DNDHandler assignment ignore
            return;
        }
        m_draggable = ((I_CmsDragHandle)source).getDraggable();

        if (m_draggable == null) {
            // cancel dragging
            return;
        }
        m_clientX = event.getClientX();
        m_clientY = event.getClientY();
        m_cursorOffsetX = CmsDomUtil.getRelativeX(m_clientX, m_draggable.getElement());
        m_cursorOffsetY = CmsDomUtil.getRelativeY(m_clientY, m_draggable.getElement());
        m_startLeft = m_draggable.getElement().getAbsoluteLeft();
        m_startTop = m_draggable.getElement().getAbsoluteTop();
        m_currentTarget = m_draggable.getParentTarget();
        m_dragHelper = m_draggable.getDragHelper(m_currentTarget);
        m_placeholder = m_draggable.getPlaceholder(m_currentTarget);
        // notifying controller, if false is returned, dragging will be canceled
        if (!m_controller.onDragStart(m_draggable, m_currentTarget, this)) {
            cancel();
            return;
        }
        m_draggable.onStartDrag(m_currentTarget);
        m_dragging = true;
        // add marker css class to enable drag and drop dependent styles
        Document.get().getBody().addClassName(
            org.opencms.gwt.client.ui.css.I_CmsLayoutBundle.INSTANCE.dragdropCss().dragStarted());
        m_previewHandlerRegistration = Event.addNativePreviewHandler(m_previewHandler);
        onMove((Event)event.getNativeEvent());
    }

    /**
     * Removes a drop target from the register.<p>
     * 
     * @param target the target to remove
     */
    public void removeTarget(I_CmsDropTarget target) {

        m_targets.remove(target);
    }

    /**
     * Sets the animation enabled.<p>
     *
     * @param animationEnabled <code>true</code> to enable the animation
     */
    public void setAnimationEnabled(boolean animationEnabled) {

        m_animationEnabled = animationEnabled;
    }

    /**
     * Sets the drag and drop controller.<p>
     *
     * @param controller the drag and drop controller to set
     */
    public void setController(I_CmsDNDController controller) {

        m_controller = controller;
    }

    /**
     * Sets the cursor offset x.<p>
     *
     * @param cursorOffsetX the cursor offset x to set
     */
    public void setCursorOffsetX(int cursorOffsetX) {

        m_cursorOffsetX = cursorOffsetX;
    }

    /**
     * Sets the cursor offset y.<p>
     *
     * @param cursorOffsetY the cursor offset y to set
     */
    public void setCursorOffsetY(int cursorOffsetY) {

        m_cursorOffsetY = cursorOffsetY;
    }

    /** 
     * Sets the draggable.<p>
     * 
     * @param draggable the draggable
     */
    public void setDraggable(I_CmsDraggable draggable) {

        m_draggable = draggable;
    }

    /**
     * Sets the drag helper element.<p>
     * 
     * @param dragHelper the drag helper element
     */
    public void setDragHelper(Element dragHelper) {

        m_dragHelper = dragHelper;
    }

    /**
     * Sets the allowed drag and drop orientation.<p>
     *
     * @param orientation the drag and drop orientation to set
     */
    public void setOrientation(Orientation orientation) {

        m_orientation = orientation;
    }

    /**
     * Sets the placeholder element.<p>
     * 
     * @param placeholder the placeholder element
     */
    public void setPlaceholder(Element placeholder) {

        m_placeholder = placeholder;
    }

    /**
     * Sets the scrolling enabled.<p>
     *
     * @param scrollEnabled <code>true</code> to enable scrolling
     */
    public void setScrollEnabled(boolean scrollEnabled) {

        m_scrollEnabled = scrollEnabled;
    }

    /**
     * Sets the start position.<p>
     * In case of a canceled drag and drop and enabled animation, 
     * the draggable helper element will be reverted to the start position.<p>
     * Values <code>&lt;0</code> will be ignored.<p>
     * 
     * @param left the left position
     * @param top the top position
     */
    public void setStartPosition(int left, int top) {

        if (left >= 0) {
            m_startLeft = left;
        }
        if (top >= 0) {
            m_startTop = top;
        }
    }

    /**
     * Clears the drag process with a move animation of the drag element to it's original position.<p>
     * 
     * @param draggable the draggable 
     * @param controller the drag and drop controller
     */
    protected void animateCancel(final I_CmsDraggable draggable, final I_CmsDNDController controller) {

        controller.onAnimationStart(draggable, null, this);
        stopDragging();
        Command callback = new Command() {

            /**
             * @see com.google.gwt.user.client.Command#execute()
             */
            public void execute() {

                controller.onDragCancel(draggable, null, CmsDNDHandler.this);
                draggable.onDragCancel();
                clear();
            }
        };
        if (!isAnimationEnabled()) {
            callback.execute();
            return;
        }
        Element parentElement;
        if (m_dragHelper != null) {
            parentElement = m_dragHelper.getParentElement();
        } else {
            return;
        }
        int endTop = m_startTop - parentElement.getAbsoluteTop();
        int endLeft = m_startLeft - parentElement.getAbsoluteLeft();
        int startTop = CmsDomUtil.getCurrentStyleInt(m_dragHelper, Style.top);
        int startLeft = CmsDomUtil.getCurrentStyleInt(m_dragHelper, Style.left);
        m_currentAnimation = new CmsMoveAnimation(m_dragHelper, startTop, startLeft, endTop, endLeft, callback);
        m_currentAnimation.run(300);
    }

    /**
     * Clears the drag process with a move animation of the drag element to the place-holder position.<p>
     * 
     * @param draggable the draggable 
     * @param target the drop target
     * @param controller the drag and drop controller
     */
    protected void animateDrop(
        final I_CmsDraggable draggable,
        final I_CmsDropTarget target,
        final I_CmsDNDController controller) {

        controller.onAnimationStart(draggable, target, this);
        stopDragging();
        Command callback = new Command() {

            /**
             * @see com.google.gwt.user.client.Command#execute()
             */
            public void execute() {

                target.onDrop(draggable);
                controller.onDrop(draggable, target, CmsDNDHandler.this);
                draggable.onDrop(target);
                clear();
            }
        };
        if (!isAnimationEnabled()) {
            callback.execute();
            return;
        }
        Element parentElement = m_dragHelper.getParentElement();

        int endTop = m_placeholder.getAbsoluteTop() - parentElement.getAbsoluteTop();
        int endLeft = m_placeholder.getAbsoluteLeft() - parentElement.getAbsoluteLeft();
        int startTop = CmsDomUtil.getCurrentStyleInt(m_dragHelper, Style.top);
        int startLeft = CmsDomUtil.getCurrentStyleInt(m_dragHelper, Style.left);
        m_currentAnimation = new CmsMoveAnimation(m_dragHelper, startTop, startLeft, endTop, endLeft, callback);
        m_currentAnimation.run(300);

    }

    /**
     * Clears all references used within the current drag process.<p>
     */
    protected void clear() {

        for (I_CmsDropTarget target : m_targets) {
            target.removePlaceholder();
        }
        if (m_dragHelper != null) {
            m_dragHelper.removeFromParent();
            m_dragHelper = null;
        }
        m_placeholder = null;
        m_currentTarget = null;
        m_draggable = null;
        Document.get().getBody().removeClassName(
            org.opencms.gwt.client.ui.css.I_CmsLayoutBundle.INSTANCE.dragdropCss().dragStarted());
        m_currentAnimation = null;
    }

    /**
     * Cancels the scroll timer and removes the timer reference.<p>
     */
    protected void clearScrollTimer() {

        if (m_scrollTimer != null) {
            m_scrollTimer.cancel();
            m_scrollTimer = null;
        }
    }

    /**
     * Execute on mouse wheel event.<p>
     * 
     * @param event the native event
     */
    protected void onMouseWheelScroll(Event event) {

        int scrollStep = event.getMouseWheelVelocityY() * 5;
        Element scrollTarget;
        if (getCurrentTarget() != null) {
            scrollTarget = getCurrentTarget().getElement();
        } else {
            scrollTarget = RootPanel.getBodyElement();
        }
        while ((scrollTarget.getScrollHeight() == scrollTarget.getClientHeight())
            && (scrollTarget != RootPanel.getBodyElement())) {
            scrollTarget = scrollTarget.getParentElement();
        }
        if (scrollTarget == RootPanel.getBodyElement()) {
            int top = Window.getScrollTop() + scrollStep;
            int left = Window.getScrollLeft();
            if (top < 0) {
                top = 0;
            }
            Window.scrollTo(left, top);
        } else {
            int top = scrollTarget.getScrollTop() + scrollStep;
            if (top < 0) {
                top = 0;
            }
            scrollTarget.setScrollTop(top);
        }
        onMove(event);
    }

    /**
     * Executed on mouse move while dragging.<p>
     * 
     * @param event the event
     */
    protected void onMove(Event event) {

        m_clientX = event.getClientX();
        m_clientY = event.getClientY();
        checkTargets();
        positionHelper();
        scrollAction();
    }

    /**
     * Executed on mouse up while dragging.<p>
     * 
     * @param event the event
     */
    protected void onUp(Event event) {

        m_clientX = event.getClientX();
        m_clientY = event.getClientY();
        if ((m_currentTarget == null) || (m_currentTarget.getPlaceholderIndex() < 0)) {

            cancel();
        } else {
            drop();
        }
    }

    /**
     * Positions an element depending on the current events client position and the cursor offset. This method assumes that the element parent is positioned relative.<p>
     */
    protected void positionHelper() {

        if (m_dragHelper == null) {
            // should never happen
            CmsDebugLog.getInstance().printLine("drag helper can not be positioned, as it is null");
            return;
        }
        Element parentElement = m_dragHelper.getParentElement();
        int left = CmsDomUtil.getRelativeX(m_clientX, parentElement) - m_cursorOffsetX;
        int top = CmsDomUtil.getRelativeY(m_clientY, parentElement) - m_cursorOffsetY;
        DOM.setStyleAttribute((com.google.gwt.user.client.Element)m_dragHelper, "left", left + "px");
        DOM.setStyleAttribute((com.google.gwt.user.client.Element)m_dragHelper, "top", top + "px");
    }

    /**
     * Method will check all registered drop targets if the element is positioned over one of them.<p>
     */
    private void checkTargets() {

        // checking current target first
        if ((m_currentTarget != null) && m_currentTarget.checkPosition(m_clientX, m_clientY, m_orientation)) {
            if (m_currentTarget.getPlaceholderIndex() < 0) {
                m_currentTarget.insertPlaceholder(m_placeholder, m_clientX, m_clientY, m_orientation);
            } else {
                m_currentTarget.repositionPlaceholder(m_clientX, m_clientY, m_orientation);
            }
            m_controller.onPositionedPlaceholder(m_draggable, m_currentTarget, this);
        } else {
            // leaving the current target
            if (m_currentTarget != null) {
                m_controller.onTargetLeave(m_draggable, m_currentTarget, this);
            }
            for (I_CmsDropTarget target : m_targets) {
                if ((target != m_currentTarget) && target.checkPosition(m_clientX, m_clientY, m_orientation)) {
                    // notifying controller, if false is returned, placeholder will not be positioned inside target 
                    if (m_controller.onTargetEnter(m_draggable, target, this)) {
                        target.insertPlaceholder(m_placeholder, m_clientX, m_clientY, m_orientation);
                        m_currentTarget = target;
                        m_controller.onPositionedPlaceholder(m_draggable, m_currentTarget, this);
                        return;
                    }
                }
            }
            // mouse position is not over any registered target
            m_currentTarget = null;
        }
    }

    /**
     * Convenience method to get the appropriate scroll direction.<p>
     * 
     * @param offset the scroll parent border offset, if the cursor is within the border offset, scrolling should be triggered
     * 
     * @return the scroll direction
     */
    private Direction getScrollDirection(int offset) {

        Element body = RootPanel.getBodyElement();
        int windowHeight = Window.getClientHeight();
        int bodyHeight = body.getClientHeight();
        if (windowHeight < bodyHeight) {
            if ((windowHeight - m_clientY < offset) && (Window.getScrollTop() < bodyHeight - windowHeight)) {
                return Direction.down;
            }
            if ((m_clientY < offset) && (Window.getScrollTop() > 0)) {
                return Direction.up;
            }
        }

        int windowWidth = Window.getClientWidth();
        int bodyWidth = body.getClientWidth();
        if (windowWidth < bodyWidth) {
            if ((windowWidth - m_clientX < offset) && (Window.getScrollLeft() < bodyWidth - windowWidth)) {
                return Direction.right;
            }
            if ((m_clientX < offset) && (Window.getScrollLeft() > 0)) {
                return Direction.left;
            }
        }

        return null;
    }

    /**
     * Handles automated scrolling.<p>
     */
    private void scrollAction() {

        if (m_scrollEnabled) {

            Direction direction = getScrollDirection(50);
            if ((m_scrollTimer != null) && (m_scrollDirection != direction)) {
                clearScrollTimer();
            }
            if ((direction != null) && (m_scrollTimer == null)) {
                m_scrollTimer = new CmsScrollTimer(RootPanel.getBodyElement(), 20, direction);
                m_scrollTimer.scheduleRepeating(10);
            }

            m_scrollDirection = direction;
        }
    }

    /**
     * Sets dragging to false and removes the event preview handler.<p>
     */
    private void stopDragging() {

        clearScrollTimer();
        m_dragging = false;
        if (m_previewHandlerRegistration != null) {
            m_previewHandlerRegistration.removeHandler();
            m_previewHandlerRegistration = null;
        }
    }
}
