mkdir tmp
pushd tmp
curl -o git-chglog.tar.gz -L https://github.com/git-chglog/git-chglog/releases/download/v0.15.1/git-chglog_0.15.1_linux_amd64.tar.gz
#curl -o git-chglog.tar.gz -L https://github.com/git-chglog/git-chglog/releases/download/v0.15.1/git-chglog_0.15.1_darwin_amd64.tar.gz
tar -zxvf git-chglog.tar.gz
cp ./git-chglog ../git-chglog
popd
rm -rf tmp
chmod u+x git-chglog
./git-chglog -o CHANGELOG.md
rm git-chglog