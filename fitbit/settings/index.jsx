function RecordingSettings(props) {
    return (
      <Page>
        <Section
          title={<Text bold align="center">Recording Settings</Text>}>
          <Toggle
            settingsKey="useMetricUnits"
            label="Use Metric Units"
          />
          <Slider
            label={'Distance Filter: '+props.settings.distanceFilterInMeters+'m'}
            settingsKey="distanceFilterInMeters"
            min="0"
            max="100"
          />
          <Slider
            label={'Time Filter: '+props.settings.timeFilterInSeconds+'s'}
            settingsKey="timeFilterInSeconds"
            min="0"
            max="60"
          />
        </Section>
      </Page>
    );
  }
  
  registerSettingsPage(RecordingSettings);