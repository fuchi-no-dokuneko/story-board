function controls(driver) {
  return {
    setTimeouts: async () => {},
    window: () => ({ setRect: ({ width, height }) => driver.sendDevToolsCommand(
      'Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false },
    ) }),
  };
}
module.exports = { controls };
