package com.jamesj.voip_phone_android.service.contact;

import android.content.Context;

import com.orhanobut.logger.Logger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @class public class ContactManager
 * @brief ContactManager class
 */
public class ContactManager {

    private static final int MAX_CONTACT_NUM = 500;
    private static final String CONTACT_FILE_NAME = "contact.txt";

    //private static ContactManager contactManager = null;

    private final LinkedList<ContactInfo> contactInfoLinkedList;
    private final ReentrantLock contactListLock = new ReentrantLock();

    private final Context context;

    ////////////////////////////////////////////////////////////////////////////////

    public ContactManager(Context context) {
        this.context = context;
        contactInfoLinkedList = new LinkedList<>();

        try {
            File contactFile = getContactFile();
            if (contactFile != null) {
                BufferedReader inFile = new BufferedReader(new FileReader(contactFile));
                String data;

                while ((data = inFile.readLine()) != null) {
                    //Logger.d("[CONTACT] [%s]", data);
                    String[] content = data.split(",");
                    if (content.length != ContactInfo.CONTACT_CONTENT_NUM) {
                        continue;
                    }

                    String name = content[0];
                    String email = content[1];
                    String mdn = content[2];
                    String sipIp = content[3];
                    int sipPort = Integer.parseInt(content[4]);

                    addContactInfo(
                            name,
                            email,
                            mdn,
                            sipIp,
                            sipPort,
                            false
                    );
                }

                inFile.close();
            } else {
                Logger.w("Fail to load the contacts. Contact file is not exist.");
            }
        } catch (Exception e) {
            Logger.w("Fail to load the contacts.", e);
        }
    }

    /*public static ContactManager getInstance () {
        if (contactManager == null) {
            contactManager = new ContactManager();
        }

        return contactManager;
    }*/

    ////////////////////////////////////////////////////////////////////////////////

    public int getContactListSize() {
        try {
            contactListLock.lock();

            return contactInfoLinkedList.size();
        } catch (Exception e) {
            Logger.w("Fail to get the contact set size.", e);
            return 0;
        } finally {
            contactListLock.unlock();
        }
    }

    public ContactInfo addContactInfo(String name, String email, String mdn, String sipIp, int sipPort, boolean isFileAdd) {
        if (mdn == null || sipIp == null || sipPort <= 0) {
            Logger.w("Fail to add the contact info. (name=%s, email=%s, mdn=%s, sipIp=%s, sipPort=%s)", name, email, mdn, sipIp, sipPort);
            return null;
        }

        if (contactInfoLinkedList.size() >= MAX_CONTACT_NUM) {
            return null;
        }

        try {
            contactListLock.lock();

            ContactInfo contactInfo = new ContactInfo(name, email, mdn, sipIp, sipPort);
            for (ContactInfo curContactInfo : contactInfoLinkedList) {
                if (curContactInfo != null) {
                    if (curContactInfo.getMdn().equals(mdn)) {
                        return null;
                    }
                }
            }

            if (contactInfoLinkedList.add(contactInfo)) {
                // Add contactInfo to the contact file.
                if (isFileAdd) {
                    File contactFile = getContactFile();
                    if (contactFile != null) {
                        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(contactFile, true));
                        bufferedWriter.append(contactInfo.toString()).append("\n");
                        bufferedWriter.close();
                    }
                }
                return contactInfo;
            } else {
                return null;
            }
        } catch (Exception e) {
            Logger.w("Fail to add the contact info. (name=%s, email=%s, mdn=%s, sipIp=%s, sipPort=%s)", name, email, mdn, sipIp, sipPort, e);
            return null;
        } finally {
            contactListLock.unlock();
        }
    }

    public void deleteContactInfo(ContactInfo contactInfo) {
        if (contactInfo == null) {
            return;
        }

        try {
            contactListLock.lock();

            if (contactInfoLinkedList.remove(contactInfo)) {
                Logger.d("Success to delete the contact info. (%s)", contactInfo);
            } else {
                Logger.w("Fail to delete the contact info. (%s)", contactInfo);
            }
        } catch (Exception e) {
            Logger.w("Fail to delete the contact info. (contactInfo=%s)", contactInfo, e);
        } finally {
            contactListLock.unlock();
        }
    }

    public ContactInfo getContactInfoByMdn(String mdn) {
        if (mdn == null) { return null; }

        try {
            contactListLock.lock();

            for (ContactInfo curContactInfo : contactInfoLinkedList) {
                if (curContactInfo != null) {
                    if (curContactInfo.getMdn().equals(mdn)) {
                        return curContactInfo;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            Logger.w("Fail to get the contact info by the mdn. (mdn=%s)", mdn, e);
            return null;
        } finally {
            contactListLock.unlock();
        }
    }

    public ContactInfo getContactInfoByIpPort(String ip, int port) {
        if (ip == null || port <= 0 || port > 65535) { return null; }

        try {
            contactListLock.lock();

            for (ContactInfo curContactInfo : contactInfoLinkedList) {
                if (curContactInfo != null) {
                    if (curContactInfo.getSipIp().equals(ip) && (curContactInfo.getSipPort() == port)) {
                        return curContactInfo;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            Logger.w("Fail to get the contact info by the mdn. (ip=%s, port=%s)", ip, port, e);
            return null;
        } finally {
            contactListLock.unlock();
        }
    }

    public ContactInfo getContactInfoByIndex(int index) {
        if (index < 0 || index >= contactInfoLinkedList.size()) {
            return null;
        }

        return contactInfoLinkedList.get(index);
    }

    public void setContactInfoByIndex(int index, ContactInfo contactInfo) {
        if (index < 0 || index >= contactInfoLinkedList.size() || contactInfo == null) {
            return;
        }

        try {
            contactListLock.lock();

            contactInfoLinkedList.set(index, contactInfo);
        } catch (Exception e) {
            Logger.w("Fail to set the contact info by the mdn. (index=%s, contactInfo=%s)", index, contactInfo, e);
        } finally {
            contactListLock.unlock();
        }
    }

    public LinkedList<ContactInfo> cloneContactInfoSet() {
        try {
            contactListLock.lock();

            return (LinkedList<ContactInfo>) contactInfoLinkedList.clone();
        } catch (Exception e) {
            Logger.w("Fail to clone the contact set.", e);
            return null;
        } finally {
            contactListLock.unlock();
        }
    }

    public int getIndexByContactInfo(ContactInfo contactInfo) {
        if (contactInfo == null) {
            return -1;
        }

        return contactInfoLinkedList.indexOf(contactInfo);
    }

    public void clearContactInfoSet() {
        try {
            contactListLock.lock();

            contactInfoLinkedList.clear();
            Logger.d("Success to clear the contact set.");
        } catch (Exception e) {
            Logger.w("Fail to clear the contact set.", e);
        } finally {
            contactListLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public File getContactFile() {
        File contactFile = null;

        try {
            contactFile = new File(context.getFilesDir() + "/" + CONTACT_FILE_NAME);
            if (!contactFile.exists() || contactFile.length() == 0) {
                // Create a new contact file.
                if (contactFile.createNewFile()) {
                    Logger.d("Created a new contact file. (%s)", contactFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return contactFile;
    }

    public boolean reWriteContactInfoFromFile(LinkedHashSet<ContactInfo> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return false;
        }

        File contactFile = getContactFile();
        if (contactFile == null) {
            Logger.w("Fail to modify the contact info from the file.");
            return false;
        }

        try {
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(contactFile))) {
                int index = 0;
                for (ContactInfo contactInfo : dataList) {
                    String contactDataString = contactInfo.toString();

                    if (index++ < dataList.size() - 1) {
                        contactDataString += "\n";
                    }

                    bufferedOutputStream.write(contactDataString.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                Logger.w("Fail to write the contact info from the file.", e);
                return false;
            }
        } catch (Exception e) {
            Logger.w("Fail to modify the contact info from the file.", e);
            return false;
        }

        return true;
    }

    public boolean removeContactInfoFromFile(ContactInfo contactInfo) {
        if (contactInfo == null) {
            return false;
        }

        File contactFile = getContactFile();
        if (contactFile == null) {
            Logger.w("Fail to remove the contact info from the file.");
            return false;
        }

        String contactDataString = contactInfo.toString();

        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(contactFile, "rw");
            List<String> dataList = new ArrayList<>();
            String data;

            while ((data = randomAccessFile.readLine()) != null) {
                if (!data.equals(contactDataString)) {
                    dataList.add(data);
                }
            }

            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(contactFile))) {
                int index = 0;
                for (String curData : dataList) {
                    if (index++ < dataList.size() - 1) {
                        curData += "\n";
                    }

                    bufferedOutputStream.write(curData.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                Logger.w("Fail to write the contact info from the file.", e);
                return false;
            }
        } catch (Exception e) {
            Logger.w("Fail to remove the contact info from the file.", e);
            return false;
        }

        return true;
    }

    public boolean clearFile() {
        File contactFile = getContactFile();
        if (contactFile == null) {
            return false;
        }

        try {
            PrintWriter writer = new PrintWriter(contactFile);
            writer.print("");
            writer.close();
        } catch (Exception e) {
            Logger.w("Fail to clear the contact file.");
            return false;
        }

        return true;
    }

}
