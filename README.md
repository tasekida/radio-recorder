# radio-recorder
K8s用インターネットラジオ録音
-  まだプロトタイプです。
    -  ロジック
        - 見直したいところ沢山
    -  K8s対応
        -  Podのタイムゾーンが30分ずれる問題
    -  [Jave2](https://github.com/a-schild/jave2)を使用させて頂いたのでGPLv3です。
        -  いずれAPLv2にしたいので、ffmpegはProcessBuilderから実行する[参考](https://www.gnu.org/licenses/gpl-faq.ja.html#GPLAndPlugins)
