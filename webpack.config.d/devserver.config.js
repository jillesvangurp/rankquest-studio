if (config.devServer) {
    // all options:
    //object { allowedHosts?, bonjour?, client?, compress?, devMiddleware?, headers?,
    // historyApiFallback?, host?, hot?, http2?, https?, ipc?, liveReload?, magicHtml?,
    // onAfterSetupMiddleware?, onBeforeSetupMiddleware?, onListening?, open?, port?, proxy?,
    // server?, setupExitSignals?, setupMiddlewares?, static?, watchFiles?, webSocketServer? }

    config.devServer.open = false
    config.devServer.hot = false
    config.devServer.liveReload = false
}
