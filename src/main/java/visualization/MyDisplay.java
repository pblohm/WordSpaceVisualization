package visualization;

import prefuse.Display;
import prefuse.Visualization;

/**
 * Display class with extra clean-up method to avoid prefuse memory leak
 *
 * @author philipp
 */
public class MyDisplay extends Display{
    
    public MyDisplay(Visualization vis){
        super(vis);
    }
    
    
    public void cleanup(){
        m_offscreen = null;
        m_queue = null;
    }
}
