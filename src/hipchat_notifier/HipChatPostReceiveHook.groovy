package hipchat_notifier
import static java.lang.System.*;
import groovy.util.logging.Slf4j
@Grapes([
	@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')
])

@Slf4j
class HipChatPostReceiveHook {
	def ConfigObject config;
	def startRevision;
	def endRevision;
	def branch;

	def HipChatPostReceiveHook(_config) {
		config = _config;
	}

	
	def trigger() {
		log.info "Sending notification to Hipchat"
	}
	

	static main(args) {

		def rev1 = args[0];
		def rev2 = args[1];
		def branch = args[2];
				
		def config = new ConfigSlurper().parse(new File('hook.properties').toURL())

		def hook = new HipChatPostReceiveHook(config);
		hook.startRevision = rev1;
		hook.endRevision = rev2;
		hook.branch = branch;
		hook.trigger();
	}
}
