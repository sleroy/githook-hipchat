#!/bin/groovy
package hipchat_notifier
import static java.lang.System.*


@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')
import groovy.util.logging.Slf4j

@GrabResolver(name="atlassian", root="https://api.bitbucket.org/1.0/repositories/collabsoft/mvn-repository/raw/releases", m2Compatible=true)
@Grab(group='com.hipchat.api', module='HipChatAPI', version='0.9.2')
import com.hipchat.api.v1.*



@Slf4j
class HipChatPostReceiveHook {
    def chatRoom
    def token
    def startRevision
    def endRevision
    def branch
//https://bitbucket.org/collabsoft/hipchatapi/src/32a8c09a603d21ca52c86a9c6351213a100e9988/src/main/java/com/hipchat/api/v1/Rooms.java?at=master

    def HipChatPostReceiveHook(_config) {
        config = _config
    }


    def trigger() {
        log.info "Process commit informations"
        log.info "Start : $startRevision"
        log.info "End : $endRevision"

        def change_type = ""
        if (startRevision.matches('0*$')) {
            change_type="create"
        } else if (endRevision.matches('0*$')) {
            change_type="delete"
        } else {
            change_type="update"
        }
        log.info "Type of change $change_type"

        
        def authorCmd = "git log --pretty=\"%an\" ${startRevision}".execute()
        authorCmd.waitFor();
        def author = authorCmd.in.text

        
        def getBodyCommand = ["git","log", "--pretty=format:Commit %h %an -%d %s (%cr)","--stat", "--abbrev-commit", "--date=relative", "${startRevision}..${endRevision}"];
        println getBodyCommand
        def command = getBodyCommand.execute()        

        command.waitFor()
        def commitMessage = command.in.text
        def commitMessageErr = command.err.text
        log.info "> $commitMessage"
        log.warn "> $commitMessageErr"
        
        log.info "Sending notification to Hipchat with token ${config.hipchat.token}"
        // Set the API key
        HipChatApi hipchatApi = HipChatApiImpl.INSTANCE
        hipchatApi.setAuthToken config.hipchat.token
        for (Room room in hipchatApi.rooms.list) {
            if (room.getName().equals(config.hipchat.chatroom)) {
                def commit = "";
                commit += """<img src="http://docs.oracle.com/cd/E10316_01/capture/capture_help/html_odcus/img/commit.gif">""";
                commit += "<p><i>$author</i> sent a new commit !<br/>"

                def message = commitMessage.split('\n');
                def wasInPre = false;
                def statRegExp = ".*\\|.*";

                for (String str in message) {
                    if (wasInPre && !str.matches(statRegExp)) {
                                wasInPre =false;
                                commit += "\n</pre><br />" 
                    }
                    if (str.matches(".*Commit .*")) {
                        commit += "<p><img src='https://www.jetbrains.com/img/webhelp/icon_showDiff.png'><b>$str</b></p><br />" 
                    } else if (str.matches("^Author.*")) {
                        commit += "<b>$str</b><br />" 
                    } else if (str.matches(".*files? changed.*")) {
                        commit += "<b>$str</b><br />" 
                    } else if (str.matches(statRegExp)) {
                        if (!wasInPre) {
                        commit += "<pre>\n$str\n" 
                        wasInPre = true;
                        } else {
                        commit += "$str\n" 
                        }
                        
                    } else {
                        commit += "$str<br />" 
                    }
                }
                
                log.info "Message  : $commit" 
                def fw =                 new FileWriter(new File("lastMessage.html"))
                fw.write(commit);
                fw.close();
                room.sendMessage "ScmManager", commit, Room.MessageFormat.html, true, Room.MessageColor.green 
                
                break
            }
        }


    }


    static main(args) {
        println args
        def rev1Proc = ("git rev-parse " + args[0]).execute()
        def rev2Proc = ("git rev-parse " + args[1]).execute()
        def chatRoom = ("git config hipchat.room ").execute()
        def token = ("git config hipchat.token ").execute()        
        rev1Proc.waitFor()
        rev2Proc.waitFor()

        def rev1 = rev1Proc.in.text.trim()
        def rev2 = rev2Proc.in.text.trim()

        def branch = args[2]



        def hook = new HipChatPostReceiveHook()
        hook.chatRoom = chatRoom
        hook.token = token;
        hook.startRevision = rev1
        hook.endRevision = rev2
        hook.branch = branch
        hook.trigger()
    }
}
