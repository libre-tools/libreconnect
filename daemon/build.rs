fn main() {
    cxx_build::bridge("src/ffi.rs")
        .file("src/daemon.cpp") // If you have any C++ source files
        .compile("libreconnect-daemon");

    println!("cargo:rerun-if-changed=src/ffi.rs");
    println!("cargo:rerun-if-changed=src/daemon.cpp");
}
