package com.zg.client;

import com.zg.bean.User;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class Client {
    private JFrame frame;

    private JTextArea textArea;
    private JTextField textField;
    private JTextField txt_port;
    private JTextField txt_hostIp;
    private JTextField txt_name;
    private JButton btn_start;
    private JButton btn_stop;
    private JButton btn_send;


    private DefaultListModel listModel;
    private boolean isConnected = false;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private MessageThread messageThread; //负责接受消息的线程
    private Map<String, User> onLineUsers = new HashMap<>(); //所有在线用户

    //构造方法
    public Client() {
        //初始化
        init();
        //写消息的文本框中按回车键时事件
        textField.addActionListener((ActionEvent e) ->{
            send(); //执行发送
        });
        //单击发送按钮时事件
        btn_send.addActionListener((ActionEvent e) -> {
            send(); //执行发送
        });
        //单击连接按钮时事件
        btn_start.addActionListener((ActionEvent e) -> {
            int port;
            if (isConnected) {
                JOptionPane.showMessageDialog(frame, "已处于连接上状态， 不要重复连接！", "错误", JOptionPane.ERROR_MESSAGE);
                return ;
            }
            try {
                try {
                    port = Integer.parseInt(txt_port.getText().trim());
                } catch (NumberFormatException e2) {
                    throw new Exception("端口号不符合要求！端口为整数！");
                }
                String hostIp = txt_hostIp.getText().trim();
                String name = txt_name.getText().trim();
                if (name.equals("") || hostIp.equals("")) {
                    throw new Exception("姓名、服务器IP不能为空!");
                }
                boolean flag = connectServer(port, hostIp, name);
                if (!flag) {
                    throw new Exception("与服务器连接失败!");
                }
                frame.setTitle(name);
                JOptionPane.showMessageDialog(frame, "成功连接！");
            }catch (Exception exc) {
                JOptionPane.showMessageDialog(frame, exc.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
            btn_start.setEnabled(false);
            btn_stop.setEnabled(true);
        });
        //单击断开按钮时事件
        btn_stop.addActionListener((ActionEvent e) -> {
            if (!isConnected) {
                JOptionPane.showMessageDialog(frame, "已处于断开状态，不要重复断开！", "错误", JOptionPane.ERROR_MESSAGE);
                return ;
            }
            try {
                boolean flag = closeConnection(); //断开连接
                if (!flag) {
                    throw new Exception("断开连接发生异常！");
                }
                JOptionPane.showMessageDialog(frame, "成功断开！");
            }catch (Exception exc) {
                JOptionPane.showMessageDialog(frame, exc.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
            btn_start.setEnabled(true);
            btn_stop.setEnabled(false);
        });

        //关闭窗口时事件
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isConnected) {
                    closeConnection(); //关闭连接
                }
                System.exit(0); //退出程序
            }
        });
    }
    /**客户端主动关闭连接*/
    private synchronized boolean closeConnection() {
        try {
            sendMessage("CLOSE"); //发送断开连接命令给服务器
            //noinspection deprecation
            messageThread.stop(); //停止接受消息线程
            //释放资源
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            isConnected = false;
            return true;
        } catch (IOException e1) {
            e1.printStackTrace();
            isConnected = true;
            return false;
        }
    }

    /**连接服务器*/
    private boolean connectServer(int port, String hostIp, String name) {
        try {
            socket = new Socket(hostIp, port); //根据端口号和服务器IP建立连接
            writer = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //发送客户端用户基本信息(用户名和IP地址)
            sendMessage(name + "@" + socket.getLocalAddress().toString());
            //开启接受消息的线程
            messageThread = new MessageThread(reader, textArea);
            messageThread.start();
            isConnected = true;
            return true;
        } catch (Exception e) {
            textArea.append("与端口号为：" + port + "        IP地址为：" + hostIp + "     的服务器连接失败！" + "\r\n");
            isConnected = false;
            return false;
        }
    }

    //执行发送
    private void send() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(frame, "还没有连接服务器，无法发送消息！", "错误", JOptionPane.ERROR_MESSAGE);
            return ;
        }
        String message = textField.getText().trim();
        if ( message.equals("")) {
            JOptionPane.showMessageDialog(frame, "消息不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
            return ;
        }
        if (message.startsWith("@")) { //悄悄话
            int index = message.indexOf(" ");
            String username = message.substring(1,index);
            sendMessage(frame.getTitle() + "@" + username + "@" + message.substring(index+1));
        } else {  //群发
            sendMessage(frame.getTitle() + "@" + "ALL" + "@" + message);
        }
        textField.setText(null);
    }
    /**发送消息*/
    private void sendMessage(String message) {
        writer.println(message);
        writer.flush();
    }
    /**初始化*/
    private void init() {
        JList userList;
        JPanel northPanel;
        JPanel southPanel;
        JScrollPane rightScroll;
        JScrollPane leftScroll;
        JSplitPane centerSplit;
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setForeground(Color.blue);
        textField = new JTextField();
        txt_port = new JTextField("6666");
        txt_hostIp = new JTextField("127.0.0.1");
        txt_name = new JTextField("zhengguo");
        btn_start = new JButton("连接");
        btn_start.setEnabled(true);
        btn_send = new JButton("发送");
        btn_stop = new JButton("断开");
        btn_stop.setEnabled(false);

        listModel = new DefaultListModel();
        userList = new JList(listModel);

        northPanel = new JPanel();
        northPanel.setLayout(new GridLayout(1,7));
        northPanel.add(new JLabel("端口"));
        northPanel.add(txt_port);
        northPanel.add(new JLabel("服务器IP"));
        northPanel.add(txt_hostIp);
        northPanel.add(new JLabel("姓名"));
        northPanel.add(txt_name);
        northPanel.add(btn_start);
        northPanel.add(btn_stop);
        northPanel.setBorder(new TitledBorder("连接信息"));

        rightScroll = new JScrollPane(textArea);
        rightScroll.setBorder(new TitledBorder("消息显示区"));
        leftScroll = new JScrollPane(userList);
        leftScroll.setBorder(new TitledBorder("在线用户"));
        southPanel = new JPanel(new BorderLayout());
        southPanel.add(textField, "Center");
        southPanel.add(btn_send, "East");
        southPanel.setBorder(new TitledBorder("写消息"));

        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        centerSplit.setDividerLocation(100);

        frame = new JFrame("客户机");
        //更改JFrame图标
        frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Client.class.getResource("/qq.jpg")));
        frame.setLayout(new BorderLayout());
        frame.add(northPanel, "North");
        frame.add(centerSplit, "Center");
        frame.add(southPanel, "South");
        frame.setSize(600,400);
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        frame.setLocation((screen_width - frame.getWidth()) / 2, (screen_height - frame.getHeight()) / 2);
        frame.setVisible(true);
    }

    // 不断接收消息的线程
    class MessageThread extends Thread {
        private BufferedReader reader;
        private JTextArea textArea;

        //接收消息线程的构造方法
        MessageThread(BufferedReader reader, JTextArea textArea) {
            this.reader = reader;
            this.textArea = textArea;
        }
        //被动的关闭连接
        synchronized  void closeCon() throws Exception {
            //清空用户列表
            listModel.removeAllElements();
            //被动的关闭连接释放的资源
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
            isConnected = false; //修改状态为断开
        }

        @Override
        public void run() {
            String message;
            while (true) {
                try {
                    message = reader.readLine();
                    StringTokenizer stringTokenizer = new StringTokenizer(message, "/@");
                    String command = stringTokenizer.nextToken();  //命令
                    switch (command) {
                        case "CLOSE":
                            textArea.append("服务器已关闭！\r\n");
                            closeCon(); //被动关闭连接
                            return ;
                        case "ADD":
                            addUser(stringTokenizer);
                            break;
                        case "DELETE":
                            deleteUser(stringTokenizer);
                            break;
                        case "USERLIST":
                            loadOnlineUserList(stringTokenizer);
                            break;
                        case "MAX":  //人数已达上限
                            textArea.append(stringTokenizer.nextToken() + stringTokenizer.nextToken() + "\r\n");
                            closeCon(); //被动关闭连接
                            JOptionPane.showMessageDialog(frame, "服务器缓冲区已满！","错误",JOptionPane.ERROR_MESSAGE);
                            return ;//结束线程
                        default:
                            textArea.append(message + "\r\n");
                            break;
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        /**加载在线用户列表*/
        private void loadOnlineUserList(StringTokenizer stringTokenizer) {
            int size = Integer.parseInt(stringTokenizer.nextToken());
            String username;
            String userIp;
            for (int i = 0; i < size; i++) {
                username = stringTokenizer.nextToken();
                userIp = stringTokenizer.nextToken();
                User user = new User(username, userIp);
                onLineUsers.put(username, user);
                listModel.addElement(username);
            }
        }

        /**有用户下线更新在线列表*/
        private void deleteUser(StringTokenizer stringTokenizer) {
            String username = stringTokenizer.nextToken();
            User user = onLineUsers.get(username);
            onLineUsers.remove(user);
            listModel.removeElement(username);
        }
        /**有用户上线更新在线列表*/
        private void addUser(StringTokenizer stringTokenizer) {
            String username ;
            String userIp;
            if ((username = stringTokenizer.nextToken()) != null && (userIp = stringTokenizer.nextToken()) != null) {
                User user = new User(username, userIp);
                onLineUsers.put(username, user);
                listModel.addElement(username);
            }
        }
    }
}
