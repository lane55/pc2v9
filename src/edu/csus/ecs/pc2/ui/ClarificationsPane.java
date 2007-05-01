package edu.csus.ecs.pc2.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.ibm.webrunner.j2mclb.util.HeapSorter;
import com.ibm.webrunner.j2mclb.util.NumericStringComparator;

import edu.csus.ecs.pc2.core.IController;
import edu.csus.ecs.pc2.core.model.Account;
import edu.csus.ecs.pc2.core.model.AccountEvent;
import edu.csus.ecs.pc2.core.model.Clarification;
import edu.csus.ecs.pc2.core.model.ClarificationEvent;
import edu.csus.ecs.pc2.core.model.ClientId;
import edu.csus.ecs.pc2.core.model.ElementId;
import edu.csus.ecs.pc2.core.model.IAccountListener;
import edu.csus.ecs.pc2.core.model.IClarificationListener;
import edu.csus.ecs.pc2.core.model.IContest;
import edu.csus.ecs.pc2.core.model.Problem;
import edu.csus.ecs.pc2.core.security.Permission;
import edu.csus.ecs.pc2.core.security.PermissionList;

/**
 * Shows clarifications in a list box.
 * 
 * @author pc2@ecs.csus.edu
 */

// $HeadURL$
public class ClarificationsPane extends JPanePlugin {

    /**
     * 
     */
    private static final long serialVersionUID = -7483784815760107250L;

    private JPanel clarificationButtonPane = null;

    private MCLB clarificationListBox = null;

    private JButton giveButton = null;

    private JButton takeButton = null;

    private JButton editButton = null;

    private JButton generateClarificationButton = null;

    private JButton filterButton = null;

    private PermissionList permissionList = new PermissionList();

    private JButton answerButton = null;

    /**
     * This method initializes
     * 
     */
    public ClarificationsPane() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     */
    private void initialize() {
        this.setLayout(new BorderLayout());
        this.setSize(new java.awt.Dimension(622,229));
        this.add(getClarificationListBox(), java.awt.BorderLayout.CENTER);
        this.add(getClarificationButtonPane(), java.awt.BorderLayout.SOUTH);

    }

    @Override
    public String getPluginTitle() {
        return "Clarifications Pane";
    }

    /**
     * This method initializes clarificationButtonPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getClarificationButtonPane() {
        if (clarificationButtonPane == null) {
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.setHgap(25);
            clarificationButtonPane = new JPanel();
            clarificationButtonPane.setLayout(flowLayout);
            clarificationButtonPane.setPreferredSize(new java.awt.Dimension(35, 35));
            clarificationButtonPane.add(getAnswerButton(), null);
            clarificationButtonPane.add(getGiveButton(), null);
            clarificationButtonPane.add(getTakeButton(), null);
            clarificationButtonPane.add(getFilterButton(), null);
            clarificationButtonPane.add(getEditButton(), null);
            clarificationButtonPane.add(getGenerateClarificationButton(), null);
        }
        return clarificationButtonPane;
    }

    /**
     * This method initializes clarificationListBox
     * 
     * @return edu.csus.ecs.pc2.core.log.MCLB
     */
    private MCLB getClarificationListBox() {
        if (clarificationListBox == null) {
            clarificationListBox = new MCLB();

            Object[] cols = { "Site", "Team", "Clar Id", "Time", "Status", "Judge", "Sent to", "Problem", "Question", "Answer" };
            clarificationListBox.addColumns(cols);

            // Sorters
            HeapSorter sorter = new HeapSorter();
            HeapSorter numericStringSorter = new HeapSorter();
            numericStringSorter.setComparator(new NumericStringComparator());

            // Site
            clarificationListBox.setColumnSorter(0, sorter, 1);

            // Team
            clarificationListBox.setColumnSorter(1, sorter, 2);

            // Clar Id
            clarificationListBox.setColumnSorter(2, numericStringSorter, 3);

            // Time
            clarificationListBox.setColumnSorter(3, numericStringSorter, 4);

            // Status
            clarificationListBox.setColumnSorter(4, sorter, 5);

            // Judge
            clarificationListBox.setColumnSorter(5, sorter, 6);

            // Sent to
            clarificationListBox.setColumnSorter(6, sorter, 7);

            // Problem
            clarificationListBox.setColumnSorter(7, sorter, 8);

            // Question
            clarificationListBox.setColumnSorter(8, sorter, 9);

            // Answer
            clarificationListBox.setColumnSorter(9, sorter, 10);

            clarificationListBox.autoSizeAllColumns();

        }
        return clarificationListBox;
    }

    public void updateClarificationRow(final Clarification clarification, final ClientId whoChangedId) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Object[] objects = buildClarificationRow(clarification, whoChangedId);
                int rowNumber = clarificationListBox.getIndexByKey(clarification.getElementId());
                if (rowNumber == -1) {
                    clarificationListBox.addRow(objects, clarification.getElementId());
                } else {
                    clarificationListBox.replaceRow(objects, rowNumber);
                }
                clarificationListBox.autoSizeAllColumns();
                clarificationListBox.sort();
            }
        });
    }

    private Object[] buildClarificationRow(Clarification clar, ClientId clientId) {

        int cols = clarificationListBox.getColumnCount();
        Object[] obj = new Object[cols];

        // Object[] cols = {"Site", "Team", "Clar Id", "Time", "Status", "Judge", "Sent to", "Problem", "Question", "Answer" };

        obj[0] = getSiteTitle(clar.getSubmitter().getSiteNumber());
        obj[1] = getTeamDisplayName(clar.getSubmitter());
        obj[2] = clar.getNumber();
        obj[3] = clar.getElapsedMins();
        
        obj[5] = "";
        if (clar.isAnswered()) {
            obj[4] = "Answered";
            obj[5] = getTeamDisplayName(clar.getWhoJudgedItId());
        } else {
            obj[4] = "Not Answered";
        }

        if (clar.isSendToAll()) {
            obj[6] = "All Teams";
        } else {
            obj[6] = getTeamDisplayName(clar.getSubmitter());
        }
        obj[7] = getProblemTitle(clar.getProblemId());
        obj[8] = clar.getQuestion();
        obj[9] = clar.getAnswer();

        return obj;
    }

    private void reloadListBox() {
        clarificationListBox.removeAllRows();
        Clarification[] clarifications = getContest().getClarifications();

        for (Clarification clarification : clarifications) {
            addClarificationRow(clarification);
        }
    }

    private void addClarificationRow(Clarification clarification) {
        Object[] objects = buildClarificationRow(clarification, null);
        clarificationListBox.addRow(objects, clarification.getElementId());
        clarificationListBox.autoSizeAllColumns();
        clarificationListBox.sort();
    }

    /**
     * 
     * 
     * @author pc2@ecs.csus.edu
     */

    // $HeadURL$
    public class ClarificationListenerImplementation implements IClarificationListener {

        public void clarificationAdded(ClarificationEvent event) {
            updateClarificationRow(event.getClarification(), event.getWhoModifiedClarification());
        }

        public void clarificationChanged(ClarificationEvent event) {
            updateClarificationRow(event.getClarification(), event.getWhoModifiedClarification());
        }

        public void clarificationRemoved(ClarificationEvent event) {
            // TODO Auto-generated method stub
        }

    }

    public void setModelAndController(IContest inModel, IController inController) {
        super.setModelAndController(inModel, inController);

        initializePermissions();
        
        getContest().addClarificationListener(new ClarificationListenerImplementation());
        getContest().addAccountListener(new AccountListenerImplementation());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateGUIperPermissions();
                reloadListBox();
            }
        });
    }

    private String getProblemTitle(ElementId problemId) {
        Problem problem = getContest().getProblem(problemId);
        if (problem != null) {
            return problem.toString();
        }
        return "Problem ?";
    }

    private String getSiteTitle(int siteNumber) {
        // TODO Auto-generated method stub
        return "Site " + siteNumber;
    }

    private String getTeamDisplayName(ClientId clientId) {
        Account account = getContest().getAccount(clientId);
        if (account != null) {
            return account.getDisplayName();
        }

        return clientId.getName();
    }

    /**
     * This method initializes getButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getGiveButton() {
        if (giveButton == null) {
            giveButton = new JButton();
            giveButton.setText("Give");
            giveButton.setMnemonic(java.awt.event.KeyEvent.VK_G);
            giveButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
                }
            });
        }
        return giveButton;
    }

    /**
     * This method initializes takeButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getTakeButton() {
        if (takeButton == null) {
            takeButton = new JButton();
            takeButton.setText("Take");
            takeButton.setMnemonic(java.awt.event.KeyEvent.VK_T);
            takeButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
                }
            });
        }
        return takeButton;
    }

    /**
     * This method initializes editButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getEditButton() {
        if (editButton == null) {
            editButton = new JButton();
            editButton.setText("Edit");
            editButton.setMnemonic(java.awt.event.KeyEvent.VK_E);
            editButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
                }
            });
        }
        return editButton;
    }

    /**
     * This method initializes generateClarificationButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getGenerateClarificationButton() {
        if (generateClarificationButton == null) {
            generateClarificationButton = new JButton();
            generateClarificationButton.setText("Generate New Clar");
            generateClarificationButton.setMnemonic(java.awt.event.KeyEvent.VK_N);
            generateClarificationButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
                }
            });
        }
        return generateClarificationButton;
    }

    /**
     * This method initializes filterButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getFilterButton() {
        if (filterButton == null) {
            filterButton = new JButton();
            filterButton.setText("Filter");
        }
        return filterButton;
    }

    private boolean isAllowed(Permission.Type type) {
        return permissionList.isAllowed(type);
    }

    private void initializePermissions() {
        Account account = getContest().getAccount(getContest().getClientId());
        permissionList.clearAndLoadPermissions(account.getPermissionList());
    }

    private void updateGUIperPermissions() {

        answerButton.setVisible(isAllowed(Permission.Type.ANSWER_CLARIFICATION));
        editButton.setVisible(isAllowed(Permission.Type.EDIT_CLARIFICATION));
        giveButton.setVisible(isAllowed(Permission.Type.GIVE_CLARIFICATION));
        takeButton.setVisible(isAllowed(Permission.Type.TAKE_CLARIFICATION));
        generateClarificationButton.setVisible(isAllowed(Permission.Type.GENERATE_NEW_CLARIFICATION));
        
    }

    /**
     * 
     * @author pc2@ecs.csus.edu
     */
    public class AccountListenerImplementation implements IAccountListener {

        public void accountAdded(AccountEvent accountEvent) {
            // ignore, doesn't affect this pane
        }

        public void accountModified(AccountEvent event) {
            // check if is this account
            Account account = event.getAccount();
            /**
             * If this is the account then update the GUI display per the potential change in Permissions.
             */
            if (getContest().getClientId().equals(account.getClientId())) {
                // They modified us!!
                initializePermissions();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateGUIperPermissions();
                    }
                });

            }
        }
    }

    /**
     * This method initializes answerButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getAnswerButton() {
        if (answerButton == null) {
            answerButton = new JButton();
            answerButton.setText("Answer");
            answerButton.setMnemonic(java.awt.event.KeyEvent.VK_A);
            answerButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
                }
            });
        }
        return answerButton;
    }

} // @jve:decl-index=0:visual-constraint="10,10"
