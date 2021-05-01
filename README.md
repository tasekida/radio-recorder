# radio-recorder

インターネットラジオ録音するためのK8sアプリケーション

## スタートガイド

スタンドアロンでも動作します。  
現時点はNHKラジオしか対応していません。  
他のラジオへの対応は未計画です。  

### 必要条件

-  k8s実行環境
-  dockerビルド環境  

## 準備
-  [プロパティファイル](src/main/resources/radio.properties)の各項目を設定してgradleでbuildします。  
```shell-session
# ./gradlew clean build
```

-  k8s環境へデプロイする場合は [yaml](manifests/k8s/nhk-english0.yaml) を用意します。  
上記は「小学生の基礎英語」を録音する設定例です。  

-  [skaffold](manifests/skaffold.yaml) も設定します。  
    -  graalvm11 + native-image でビルドする場合は下記  
`dockerfile: manifests/docker/Dockerfile.graalvm11`
    -  openjdk11 + jlink でビルドする場合は下記  
`dockerfile: manifests/docker/Dockerfile.openjdk11`

## デプロイ

下記のコマンドで Kubernetes Cluster へデプロイします。
```shell-session
# skaffold dev -f manifests/skaffold.yaml --cleanup=false
# ctrl + c
```

## アプリケーション

![](doc/summary.png)
![](doc/radio-racorder.png)
![](doc/classdiagram.png)

## ライセンス

このプロジェクトは Apache License, Version 2.0 ライセンスの元にライセンスされています。   
詳細は [LICENSE.md](LICENSE.md) をご覧ください。
