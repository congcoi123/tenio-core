package com.tenio.core.configuration.kcp;

public enum KcpProfile {

  NORMAL_MODE {
  },

  BOOSTER_MODE {
    @Override
    public int getNodelay() {
      return 1;
    }

    @Override
    public int getUpdateInterval() {
      return 10;
    }

    @Override
    public int getFastResend() {
      return 2;
    }

    @Override
    public int getCongestionControl() {
      return 1;
    }
  };

  public int getNodelay() {
    return 0;
  }

  public int getUpdateInterval() {
    return 40;
  }

  public int getFastResend() {
    return 0;
  }

  public int getCongestionControl() {
    return 0;
  }

  public int getSendWindowSize() {
    return 32;
  }

  public int getReceiveWindowSize() {
    return 32;
  }

  public int getMtu() {
    return 1400;
  }
}
