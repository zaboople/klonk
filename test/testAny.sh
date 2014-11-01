source lib/classpath.sh
ant config.test compile && java -Xshare:off -Xms32m "$@"
