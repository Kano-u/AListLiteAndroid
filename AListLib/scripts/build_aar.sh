cd ../sources
gomobile bind -ldflags "-s -w" -v -androidapi 21 -target=android/arm64 "github.com/OpenListTeam/OpenList/v4/alistlib" "github.com/OpenListTeam/OpenList/v4/alitvlib"
mkdir -p ../../app/libs/
cp -f ./alistlib.aar ../../app/libs/
# app 同时用到了 alistlib 与 alitvlib，把生成的 AAR 全部复制到 app/libs
cp -f ./*.aar ../../app/libs/
