function RecordingSettings(props) {
    return (
      <Page>
        <Section
          title={<Text bold align="center">Recording Settings</Text>}>
          <Toggle
            settingsKey="useMetricUnits"
            label="Use Metric Units"
          />
        </Section>
      </Page>
    );
  }
  
  registerSettingsPage(RecordingSettings);