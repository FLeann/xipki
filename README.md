# XiPKI Toolkits
XiPKI (e**X**tensible s**I**mple **P**ublic **K**ey **I**nfrastructure) SDK

## License
* The Apache Software License, Version 2.0

## Owner
Lijun Liao (lijun.liao -A-T- gmail -D-O-T- com), [LinkedIn](https://www.linkedin.com/in/lijun-liao-644696b8)

## Support
Just drop me an email.

## Build

- Prepare dependency XiSCEP (optional, required if not done before)

  - Get a copy of XiSCEP code
    ```sh
    git clone https://github.com/xipki/xiscep.git
    ```
  - Build and install maven artifacts
    In the folder xiscep, call `mvn install -DskipTests`

- Prepare dependency XiTK (optional, required if not done before)

  - Get a copy of XiSCEP code
    ```sh
    git clone https://github.com/xipki/xitk.git
    ```
    The option `--recursive` is required to checkout the submodules.
  - Build and install maven artifacts
    In the folder xitk, call `mvn install -DskipTests`

- Build the project

  - Get a copy of project code
    ```sh
    git clone https://github.com/xipki/xisdk.git
    ```

  - Build and install maven artifacts
    In folder `xitk`
    ```sh
    mvn clean install
    ```

