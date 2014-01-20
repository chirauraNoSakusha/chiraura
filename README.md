ちらしの裏
==========

http://chiraura0.web.fc2.com/

使い方
----------

### コンパイル

src のあるディレクトリで、bin の中にクラスファイルをつくるなら、

    $ javac -sourcepath src -d bin src/nippon/kawauso/chiraura/a/A.java

とします。
Windows など文字コードが UTF-8 ではない環境では、

    $ javac -sourcepath src -d bin -encoding UTF-8 src/nippon/kawauso/chiraura/a/A.java

とします。
### 実行

    $ java -classpath bin nippon.kawauso.chiraura.a.A

設定方法は http://chiraura0.web.fc2.com/ をご覧下さい。
