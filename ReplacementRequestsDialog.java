package com.smartwarehouse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Admin review window for customer replacement requests. */
public class ReplacementRequestsDialog extends JDialog {

    private final ReplacementDAO dao;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Request", "Order", "User ID", "Username", "Product",
                    "Qty", "Reason", "Status", "Requested At"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public ReplacementRequestsDialog(Frame owner) {
        super(owner, "Product Replacement Requests", true);
        ReplacementDAO createdDao;
        try {
            createdDao = new ReplacementDAO();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner,
                    "Could not open replacement requests:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            dao = null;
            return;
        }
        dao = createdDao;

        setSize(1050, 480);
        setMinimumSize(new Dimension(850, 400));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh");
        JButton approve = new JButton("Approve Selected");
        JButton reject = new JButton("Reject Selected");
        JButton close = new JButton("Close");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(refresh);
        buttons.add(approve);
        buttons.add(reject);
        buttons.add(close);
        add(buttons, BorderLayout.SOUTH);

        refresh.addActionListener(e -> loadRequests());
        approve.addActionListener(e -> changeStatus("APPROVED"));
        reject.addActionListener(e -> changeStatus("REJECTED"));
        close.addActionListener(e -> dispose());
        loadRequests();
    }

    private void loadRequests() {
        if (dao == null) {
            return;
        }
        model.setRowCount(0);
        try {
            List<Object[]> requests = dao.getAllRequests();
            for (Object[] request : requests) {
                model.addRow(request);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load requests:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeStatus(String newStatus) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a request first.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        int requestId = ((Number) model.getValueAt(row, 0)).intValue();
        try {
            dao.updateRequestStatus(requestId, newStatus);
            loadRequests();
            JOptionPane.showMessageDialog(this,
                    "Replacement request marked " + newStatus + ".",
                    "Request Updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not update request:\n" + ex.getMessage(),
                    "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
