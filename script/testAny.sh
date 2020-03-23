cd $(dirname $0)/.. || exit 1
source lib/classpath.sh || exit 1
#java -version || exit 1
ant config.test compile && java -Xshare:off -Xms32m "$@"
