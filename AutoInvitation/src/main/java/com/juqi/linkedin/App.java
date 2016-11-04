package com.juqi.linkedin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class App {

    private static List<String> memberId = new ArrayList<String>();
    private static List<String> entityId = new ArrayList<String>();

    private printOutput getSteamWrapper(InputStream is, String type) {
        return new printOutput(is);
    }

    private class printOutput extends Thread {
        InputStream is = null;
        private String mid = null;
        private String eid = null;

        private void setMid(String mid) {
            this.mid = mid; }

        private void setEid(String eid) {
            this.eid = eid; }

        private String getMid() {
            return mid; }

        private String getEid() {
            return eid; }

        printOutput(InputStream is) {
            this.is = is; }

        public void run() {
            String s = null;

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                while ((s = br.readLine()) != null) {
                    System.out.println(s);
                    if (s.trim().startsWith("Location: ")) {
                        setEid(s.trim().substring(10));
                        entityId.add(this.getEid());
                        //System.out.println("=======entityID is: " + this.getEid());
                    }
                    if (s.trim().startsWith("\"memberId\": ")) {
                        s = s.trim();
                        setMid(s.substring(11, s.length() - 1));
                        memberId.add(this.getMid());
                        //System.out.println("memberId size is: " + entityId.size());
                    }
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private String getInviteeMid () {
        //implementation of getting invitee's memberId
        return null;
    }

    public static void main(String[] args) {

        //curli command to generate new user, entityId is caught by the printOutput
        final String[] getNewUserCmd = {"curli", "-i", "--pretty-print", "d2://datagenEntities", "-X", "POST", "-H", "X-RestLi-Method:create", "--data", "{\"data\": {\"com.linkedin.datagensvc.MemberData\": {}}, \"type\": \"REGMEMBER\", \"fabricUrn\": \"urn:li:fabric:EI3\"}"};
        //curli command to get memberId with entityId
        final String[] getEntityIdCmd = {"curli", "http://lca1-app0763.stg.linkedin.com:10558/datagen"};
        //curli command to send invitation between inviter and invitee
        final String[] sendInviteCmd = {"curli", "--pretty-print", "d2://invitationsV2?doBypass=false", "-X", "POST", "-H", "X-RestLi-Method:create", "--data", ""};
        String[] dataObject = {"{\"invitee\": ", "inviteeMid", ", \"inviter\": ", "inviterMid", "} -i"};

        Runtime rt = Runtime.getRuntime();
        App ai = new App();

        printOutput newUserError;
        printOutput newUserOutput;
        printOutput getIdError;
        printOutput getIdOutput;
        printOutput sendInviteError;
        printOutput sendInviteOutput;

        long before = System.currentTimeMillis();


        try {
            for(int i = 0; i < 20; i++) {
                //create process to excuess command
                Process proc = rt.exec(getNewUserCmd);

                //get error and output message and print out
                newUserError = ai.getSteamWrapper(proc.getErrorStream(), "ERROR");
                newUserOutput = ai.getSteamWrapper(proc.getInputStream(), "OUTPUT");
                newUserError.start();
                newUserOutput.start();

                proc.waitFor();
                System.out.println("Stage 1 takes: " + (System.currentTimeMillis() - before) + " Milliseconds\n");

                getEntityIdCmd[1] += entityId.get(entityId.size() - 1).trim();
                proc = rt.exec(getEntityIdCmd);
                getIdError = ai.getSteamWrapper(proc.getErrorStream(), "ERROR");
                getIdOutput = ai.getSteamWrapper(proc.getInputStream(), "OUTPUT");
                getIdError.start();
                getIdOutput.start();
                getEntityIdCmd[1] = "http://lca1-app0763.stg.linkedin.com:10558/datagen";

                proc.waitFor();
                System.out.println("Stage 2 takes: " + (System.currentTimeMillis() - before) + " Milliseconds\n");

                dataObject[1] = "\"urn:li:member:90794394\"";
                dataObject[3] = "\"urn:li:member:" + memberId.get(memberId.size() - 1).trim() + "\"";

                for (int j = 0; j < dataObject.length; j++) {
                    sendInviteCmd[sendInviteCmd.length - 1] += dataObject[j];
                }

                proc = rt.exec(sendInviteCmd);
                sendInviteError = ai.getSteamWrapper(proc.getErrorStream(), "ERROR");
                sendInviteOutput = ai.getSteamWrapper(proc.getInputStream(), "OUTPUT");
                sendInviteError.start();
                sendInviteOutput.start();

                proc.waitFor();
                System.out.println("Stage 3 takes: " + (System.currentTimeMillis() - before) + " Milliseconds");

                sendInviteCmd[8] = "";

                rt.gc();
            }

            long after = System.currentTimeMillis();

            System.out.println("Total takes: " + (after - before) + " Milliseconds");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
