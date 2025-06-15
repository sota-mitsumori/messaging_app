# Messaging App
ソフトウェア制作 5-A 制作物レポジトリ
村瀬優介　初めてのブランチです  
メッセージとともに送った時刻が表示されます　　あと日本語も送れます  

＃＃実行時の注意##    
vscode上のターミナルでは日本語が文字化けするので各自のコマンドプロンプトで実行してください（Windowsキー + R → cmd → Enterでコマンドプロンプトが開きます）  
ターミナルを二つ開き  
１つめは  
chcp 65001  
javac *.java  
java -Dfile.encoding=UTF-8 JabberServer  


もう一個でも  
chcp 65001  
java -Dfile.encoding=UTF-8 JabberClient


こうしてください（こうしないと文字化けします）

