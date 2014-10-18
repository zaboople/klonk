source lib/classpath.sh
ant config.test compile && java -Xshare:off -Xms6m "$@"
