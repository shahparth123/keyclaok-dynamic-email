db.object_configurations.find({'metadata.tenant': {$eq: "rosadiaz"}}).pretty()


db.users.find({'metadata.tenant': 'admin'}).forEach((record) => print(record.metadata.system.audit.createdAt))

db.object_buttons.find({'spec.objectConfigurationUri': {$eq: "mavq:objects://tenants/testtenant/object_configurations/devices"}}).forEach((record) => print(record.spec.name))
var count = 0;
db.object_configurations.find().forEach((obj) => {
    db.object_buttons.find({'spec.objectConfigurationUri': {$eq: obj.metadata.system.selfLink}}).forEach((record) => {
            var spec = record.spec;
            var metadata = record.metadata;
            var temp = String(spec.name);
            temp = temp.replace(/ /g, "");
            temp = temp.replace(/_/g, "");
            if (spec.type == 'PLATFORM_CHANGE_RECORD_TYPE' && spec.name != 'Change Record Type') {
                print(temp + "||||||||||");
                spec.name = 'Change Record Type';
                metadata.uid = obj.metadata.uid + '-n-change-record-type';
                print('initial: ' + metadata.system.selfLink);
                metadata.system.selfLink = 'mavq:objects://tenants/' + metadata.tenant + '/object_buttons/' + obj.metadata.uid + '-n-change-record-type';
                print('duplicateCount : ' + db.object_buttons.find({'metadata.system.selfLink': {$eq: metadata.system.selfLink}}).count());
                print('final : ' + metadata.system.selfLink);
                print('OC_URI :', obj.metadata.system.selfLink);

                count++;
            } else if (spec.type == 'PLATFORM_DELETE' && spec.name != 'Delete' && temp.includes("delete")) {
                spec.name = 'Delete';
                metadata.uid = obj.metadata.uid + '-n-delete';
                print('initial: ' + metadata.system.selfLink);
                metadata.system.selfLink = 'mavq:objects://tenants/' + metadata.tenant + '/object_buttons/' + obj.metadata.uid + '-n-delete';
                print('duplicateCount : ' + db.object_buttons.find({'metadata.system.selfLink': {$eq: metadata.system.selfLink}}).count())
                print('final : ' + metadata.system.selfLink);
                print('OC_URI :', obj.metadata.system.selfLink);

                count++;
            } else if (spec.type == 'PLATFORM_EDIT' && spec.name != 'Edit' && temp.includes("edit")) {
                spec.name = 'Edit';
                metadata.uid = obj.metadata.uid + '-n-edit';
                print('initial: ' + metadata.system.selfLink);
                metadata.system.selfLink = 'mavq:objects://tenants/' + metadata.tenant + '/object_buttons/' + obj.metadata.uid + '-n-edit';
                print('duplicateCount : ' + db.object_buttons.find({'metadata.system.selfLink': {$eq: metadata.system.selfLink}}).count())
                print('final : ' + metadata.system.selfLink);
                print('OC_URI :', obj.metadata.system.selfLink);

                count++;
            }
            //db.object_buttons.updateOne({ _id: record._id }, { $set: { 'spec': spec , 'metadata' : metadata } });
        }
    )
});
print(count);