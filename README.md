# radio-recorder
K8s用インターネットラジオ録音
-  まだプロトタイプです。
    -  ロジック
        - 見直したいところ沢山
    -  肝心のK8s未対応。
        -  DaemonSet、CronJob両対応にする（未対応）
        -  YAMLを書く
    -  [Jave2](https://github.com/a-schild/jave2)を使用させて頂いたのでGPLv3です。
        -  いずれAPLv2にしたいので、ffmpegはProcessBuilderから実行する[参考](https://www.gnu.org/licenses/gpl-faq.ja.html#GPLAndPlugins)
