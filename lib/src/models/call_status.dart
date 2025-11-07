enum CallStatus {
  ringing,
  answering,
  answered,
  declined,
  cancelled,
  timeout,
  missed;

  static CallStatus fromName(String value) {
    return CallStatus.values.firstWhere(
      (status) => status.name == value,
      orElse: () => CallStatus.ringing,
    );
  }
}

