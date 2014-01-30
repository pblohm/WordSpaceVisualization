package visualization;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import prefuse.action.layout.Layout;
import prefuse.visual.DecoratorItem;
import prefuse.visual.VisualItem;

/**
 *
 * @author philipp
 */
public class MyLayout extends Layout {

    public MyLayout(String group) {
        super(group);
    }

    @Override
    public void run(double frac) {

        // m_vis and m_group are inherited.  They are the Visualization object and the string label for the group to be positioned by the layout.


        Iterator iter = m_vis.items(m_group);


        // For all of the objects in the group


        while (iter.hasNext()) {


            // Get the decorator, get the item it decorates, and get the bounding box for the item.

            DecoratorItem decorator = (DecoratorItem) iter.next();
            VisualItem decoratedItem = decorator.getDecoratedItem();
            Rectangle2D bounds = decoratedItem.getBounds();

            // Get the center point for the object, 
            // and then set that point as the decorator's location.        


            double x = bounds.getCenterX();
            double y = bounds.getCenterY();
            setX(decorator, null, x);
            setY(decorator, null, y);
        }
    }
}
