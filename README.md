# TPTP2Graph

The purpose of this package is to transform [TPTP](https://tptp.org) formulae parsed by [this package](https://github.com/inpefess/tptp-grpc) to graphs stored in the [protobuf](https://protobuf.dev) format to be usable by the [Deep Graph Library](https://www.dgl.ai/).

# How to build

This package uses Java 11 and [Gradle](https://gradle.org/). It also depends on a [package](https://github.com/inpefess/tptp-grpc/packages/1854169) hosted at the GitHub packages Maven [registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry). To get it, first generate GitHub personal access token. Then add you GitHub user name as ``gpr.user`` and you personal access token as ``grp.key`` to ``gradle.properties``.

```sh
git clone https://github.com/inpefess/tptp2graph.git
cd tptp2graph
./gradlew build
```

# How to run the a transofrmation

Assuming you have parsed protobuf binaries generated by [tptp-grpc](https://github.com/inpefess/tptp-grpc) in the folder ``proto-binaries``:

```sh
find absolute_path_to_proto-binares -name "*.pb" > proto-binaries-list.txt
mkdir output
./gradlew run -PmainClassToRun=io.github.inpefess.tptp2graph.tptp2graph.TptpProto2Graph --args="absolute_path_to_proto-binaries-list.txt absolute_path_to_output_folder"
```
