package visualization;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

public class CommitAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    
    AutoComplete ac;
    
    public CommitAction(AutoComplete ac){
        this.ac = ac;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
      if (ac.isCompleteMode()) {
        int pos = ac.textField.getSelectionEnd();
        StringBuffer sb = new StringBuffer(ac.textField.getText());
        sb.insert(pos, " ");
        ac.textField.setText(sb.toString());
        ac.textField.setCaretPosition(pos + 1);
        ac.setInsertMode();
      } else {
        ac.textField.replaceSelection("\t");
      }
    }
    
}
