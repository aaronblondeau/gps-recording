#  App Build Notes

For classes like Track, GPSRecordingStore etc to be visible to unit tests : 
Click on each class, then in the File Inspector, in the Target Membeship field, check the box for the unit tests target

For NSPersistentContainer to work with unit tests, we needed
A NSManagedObjectModel (mergedModel) that is from: the unit tests bundle
Use a NSPersistentStoreDescription with type NSInMemoryStoreType

For the distance filter custom table cell, in order to get it to dynamically size to a custom height I had to wrap all views in a parent (StackView) and then set that parent's top and bottom constraints to the Superview
