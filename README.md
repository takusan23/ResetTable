# ResetTable

![Imgur](https://imgur.com/BfpHj2r.gif)

![Imgur](https://imgur.com/YXRxKIN.png)

`RevereseCraft`のようなもの。間違えてクラフトしたときに戻せるテーブル。  
久しぶりにMOD作りたくなったので。

ホッパーで搬入、搬出機能もあります  
![Imgur](https://imgur.com/eBSK2rK.png)

# MOD導入方法
- 最新の Fabric Loader を入れる
- 以下の前提MODをmodsの中に入れる
  - Fabric API
    - https://www.curseforge.com/minecraft/mc-mods/fabric-api/files
  - Fabric Language Kotlin
    - Kotlinという言語で書かれたため、他のMODと違い必要になる。
    - https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin/files
  - Mod本体
- 以上

## 開発者向け

自分でこのMODをビルドしたり改造して遊びたい方向け情報。  
FabricとKotlinでできている。

### Gitのブランチ

各バージョンのブランチがあります。

### 必要なもの

- Java 17 以降
  - 私は `Eclipse Adoptium 17` 使います
- IntelliJ IDEA

## 開発環境構築

- 必要なものを用意します
- このリポジトリをクローンします
    - ソースコードのzipをダウンロードしても良い？
- IDEAで開きます
- Gradleが終わるまで待ちます
- 作業が終わったら一旦閉じる
- 再度プロジェクトを開くと、`Minecraft Client`が実行可能になっているので実行する
- マイクラが起動する！
