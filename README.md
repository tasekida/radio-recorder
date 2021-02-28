# radio-recorder
K8s用インターネットラジオ録音
-  まだプロトタイプです。
    -  肝心のK8s未対応。
        -  DaemonSetにするかCronJobにするか悩んでます。それによってプログラムの引数が決まります。
        -  録音ファイルの保存先も決まっていません。
    -  [Jave2](https://github.com/a-schild/jave2)を使用させて頂いたのでGPLv3です。
        -  いずれAPLv2にしたいので、ffmpegはRuntime.exec()から実行するかも？[参考](https://www.gnu.org/licenses/gpl-faq.ja.html#GPLAndPlugins)
