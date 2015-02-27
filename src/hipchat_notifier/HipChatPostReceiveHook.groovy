package hipchat_notifier
import static java.lang.System.*


@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')
import groovy.util.logging.Slf4j

@GrabResolver(name="atlassian", root="https://api.bitbucket.org/1.0/repositories/collabsoft/mvn-repository/raw/releases", m2Compatible=true)
@Grab(group='com.hipchat.api', module='HipChatAPI', version='0.9.2')
import com.hipchat.api.v1.*



@Slf4j
class HipChatPostReceiveHook {
	def ConfigObject config
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
		
		
		def command = "git log --pretty=\"%s %b\" ${startRevision}..${endRevision}".execute()		
		command.waitFor()
		def commitMessage = command.in.text
		log.info "> $commitMessage"
		
		log.info "Sending notification to Hipchat with token ${config.hipchat.token}"
		// Set the API key
		HipChatApi hipchatApi = HipChatApiImpl.INSTANCE
		hipchatApi.setAuthToken config.hipchat.token
		for (Room room in hipchatApi.rooms.list) {
			if (room.getName().equals(config.hipchat.chatroom)) {
				def commit = "<p><i>$author</i> push new commits !<br/><ul>"
				def message = commitMessage.split('\n');
				for (String str in message) {
					commit += "<li><b>$str</b></li>" 
				}
				commit += "</ul>"
				log.info "Message  : $commit" 
				room.sendMessage "ScmManager", commit, Room.MessageFormat.html, true, Room.MessageColor.gray 
				
				break
			}
		}


	}


	static main(args) {
		println args
		def rev1Proc = ("git rev-parse " + args[0]).execute()
		def rev2Proc = ("git rev-parse " + args[1]).execute()
		rev1Proc.waitFor()
		rev2Proc.waitFor()

		def rev1 = rev1Proc.in.text.trim()
		def rev2 = rev2Proc.in.text.trim()

		def branch = args[2]

		def config = new ConfigSlurper().parse(new File('hook.properties').toURL())

		def hook = new HipChatPostReceiveHook(config)
		hook.startRevision = rev1
		hook.endRevision = rev2
		hook.branch = branch
		hook.trigger()
	}
}
