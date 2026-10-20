function fn() {
  var port = karate.properties['karate.port'] || '8080';
  var today = karate.properties['karate.today'] || '2030-01-15';
  var LocalDate = Java.type('java.time.LocalDate');
  var base = LocalDate.parse(today);

  var config = {
    baseUrl: 'http://localhost:' + port,
    apiUrl: 'http://localhost:' + port + '/api/tasks',
    today: today,
    plusDays: function (n) { return base.plusDays(n).toString(); },
    uniqueTitle: function (prefix) { return prefix + ' ' + java.util.UUID.randomUUID(); }
  };

  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 10000);
  karate.configure('logPrettyRequest', true);
  karate.configure('logPrettyResponse', true);
  return config;
}
